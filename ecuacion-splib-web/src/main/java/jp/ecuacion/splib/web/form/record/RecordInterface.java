/*
 * Copyright © 2012 ecuacion.jp (info@ecuacion.jp)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jp.ecuacion.splib.web.form.record;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jp.ecuacion.splib.web.bean.HtmlItem;
import jp.ecuacion.splib.web.bean.HtmlItemNumber;
import jp.ecuacion.splib.web.util.SplibRecordUtil;
import jp.ecuacion.splib.web.util.SplibSecurityUtil;
import jp.ecuacion.splib.web.util.SplibSecurityUtil.RolesAndAuthoritiesBean;

/**
 * Has features related web environment.
 */
public interface RecordInterface {

  /**
   * Returns HtmlItem.
   * 
   * @return HtmlItem[]
   */
  HtmlItem[] getHtmlItems();

  /**
   * Returns HtmlItem in respond to the specified fieldId.
   * 
   * @param fieldId fieldId
   * @return HtmlItem
   */
  default HtmlItem getHtmlItem(String fieldId) {
    return new SplibRecordUtil().getHtmlItem(getHtmlItems(), fieldId);
  }

  /**
   * Returns whether the itemId needs comma.
   * 
   * @param itemId itemId
   * @return boolean
   */
  default boolean needsCommas(String itemId) {
    HtmlItem item = Arrays.asList(getHtmlItems()).stream()
        .collect(Collectors.toMap(e -> e.getItemIdField(), e -> e)).get(itemId);

    if (item == null || !(item instanceof HtmlItemNumber)) {
      return false;
    }

    HtmlItemNumber numItem = (HtmlItemNumber) item;
    return numItem.getNeedsCommas();
  }

  /**
   * labelFieldNameを返す.
   * HtmlItemに指定したfieldNameをitemNameに指定すると、field_name.propertiesで解決できる形のitemNameが取得できる。
   * 
   * <p>HtmlItem上の(itemName, labelItemname)は、rootRecord配下の属性の場合("id", "name")のように属性名単体で定義されているが、
   * getLabelItemName()にはrootRecordNameの引数も渡しており、戻り値は"acc.accName"のようにそのままfield名として取得できる仕様としている。
   * </p>
   * 
   * <p>relationがある場合は、("accGroup.id", "accGroup.name")のようにHtmlItem上設定される。
   * この場合、戻り値に"acc."を付加するとfield_names.propertiesで解決できないので"acc."の付加はしない。
   * </p>
   * 
   * <p>relationがある場合を含めて、各componentのitemNameに指定する文字列は、必ずHtmlItemに指定したitemNameと同一となる。
   * </p>
   */
  default String getDisplayName(String rootRecordId, String fieldId) {
    HtmlItem[] htmlItems = getHtmlItems() == null ? new HtmlItem[] {} : getHtmlItems();

    // rootRecordNameがaccの場合に、例外処理でエラーメッセージに項目名を出す際の処理は、itemNameが「acc.accId」となってしまう。
    // この場合でも対応できるように、itemNameの頭がrootRecordName + '.'の場合はそれを取り除く処理を行う。
    if (fieldId.startsWith(rootRecordId + ".")) {
      fieldId = fieldId.substring((rootRecordId + ".").length());
    }

    Map<String, String> displayNameIdMap = Arrays.asList(htmlItems).stream()
        .collect(Collectors.toMap(e -> e.getItemIdField(),
            e -> e.getItemIdFieldForName() == null ? e.getItemIdField()
                : e.getItemIdFieldForName()));
    String displayNameId = displayNameIdMap.get(fieldId);

    // htmlItems上で定義がない場合 /
    // htmlItems上でlabelItemNameが未定義の場合はlabelItemNameがnullになるが、その場合はもとのfieldNameの値を入れておく
    displayNameId = displayNameId == null ? fieldId : displayNameId;

    // htmlItems上、関連を使用している場合はaccGroup.nameのようにfieldName自体にentityNameを含む場合があるので考慮
    return displayNameId.contains(".") ? displayNameId : rootRecordId + "." + displayNameId;
  }

  /**
   * htmlItemsについて、個別機能のlistと共通のlistをmergeさせるために使用する.
   * あくまでutilレベルなので個別処理にしても良いのだが、極力個別コードを減らしたいので本クラスに保持する。
   */
  default HtmlItem[] mergeHtmlItems(HtmlItem[] fields1, HtmlItem[] fields2) {
    List<HtmlItem> list = new ArrayList<>(Arrays.asList(fields1));

    // common側と個別側で同一項目が定義されている場合はエラーとする
    List<String> field1IdList =
        Arrays.asList(fields1).stream().map(e -> e.getItemIdField()).toList();

    for (String field2Id : Arrays.asList(fields2).stream().map(e -> e.getItemIdField()).toList()) {
      if (field1IdList.contains(field2Id)) {
        throw new RuntimeException(
            "'id' of HtmlItem[] duplicated with commonHtmlItems. key: " + field2Id);
      }
    }

    list.addAll(Arrays.asList(fields2));

    return list.toArray(new HtmlItem[list.size()]);

  }

  /**
   * Obtrains NotEmpty fields.
   * 
   * @param loginState loginState
   * @param bean bean
   * @return {@code List<String>}
   */
  default List<String> getNotEmptyFields(String loginState, RolesAndAuthoritiesBean bean) {
    List<String> list = new ArrayList<>();

    HtmlItem[] htmlItems = getHtmlItems();
    for (HtmlItem field : htmlItems) {
      if (field.getIsNotEmpty(loginState, bean)) {
        list.add(field.getItemIdField());
      }
    }

    return list;
  }

  /**
   * Returns whether isNotEmpty or not.
   * 
   * @param fieldId fieldId
   * @param loginState loginState
   * @param rolesOrAuthoritiesString rolesOrAuthoritiesString
   * @return boolean
   */
  default boolean isNotEmpty(String fieldId, String loginState, String rolesOrAuthoritiesString) {
    SplibSecurityUtil.RolesAndAuthoritiesBean bean =
        new SplibSecurityUtil().getRolesAndAuthoritiesBean(rolesOrAuthoritiesString);
    return getNotEmptyFields(loginState, bean).contains(fieldId);
  }
}
