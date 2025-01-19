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
import jp.ecuacion.splib.web.bean.HtmlField;
import jp.ecuacion.splib.web.bean.HtmlFieldNumber;
import jp.ecuacion.splib.web.util.SplibRecordUtil;
import jp.ecuacion.splib.web.util.SplibSecurityUtil;
import jp.ecuacion.splib.web.util.SplibSecurityUtil.RolesAndAuthoritiesBean;

/**
 * record自体はweb, batchなどの種類によらず一定の形でcode-generatorから出力される。
 * が、一方で、webの場合に追加されていて欲しい機能もあるため、それをinterfaceの形でまとめておく。 webで使用するrecordは皆これを実装する形とする。
 */
public interface RecordInterface {

  HtmlField[] getHtmlFields();

  default HtmlField getHtmlField(String fieldId) {
    return new SplibRecordUtil().getHtmlField(getHtmlFields(), fieldId);
  }

  default boolean needsCommas(String itemName) {
    HtmlField item = Arrays.asList(getHtmlFields()).stream()
        .collect(Collectors.toMap(e -> e.getId(), e -> e)).get(itemName);

    if (item == null || !(item instanceof HtmlFieldNumber)) {
      return false;
    }

    HtmlFieldNumber numItem = (HtmlFieldNumber) item;
    return numItem.getNeedsCommas();
  }

  /**
   * labelFieldNameを返す。
   * HtmlItemに指定したfieldNameをitemNameに指定すると、field_name.propertiesで解決できる形のitemNameが取得できる。
   * 
   * <p>
   * HtmlItem上の(itemName, labelItemname)は、rootRecord配下の属性の場合("id", "name")のように属性名単体で定義されているが、
   * getLabelItemName()にはrootRecordNameの引数も渡しており、戻り値は"acc.accName"のようにそのままfield名として取得できる仕様としている。
   * </p>
   * 
   * <p>
   * relationがある場合は、("accGroup.id", "accGroup.name")のようにHtmlItem上設定される。
   * この場合、戻り値に"acc."を付加するとfield_names.propertiesで解決できないので"acc."の付加はしない。
   * </p>
   * 
   * <p>
   * relationがある場合を含めて、各componentのitemNameに指定する文字列は、必ずHtmlItemに指定したitemNameと同一となる。
   * </p>
   */
  default String getLabelItemName(String rootRecordname, String itemName) {
    HtmlField[] htmlItems = getHtmlFields() == null ? new HtmlField[] {} : getHtmlFields();

    // rootRecordNameがaccの場合に、例外処理でエラーメッセージに項目名を出す際の処理は、itemNameが「acc.accId」となってしまう。
    // この場合でも対応できるように、itemNameの頭がrootRecordName + '.'の場合はそれを取り除く処理を行う。
    if (itemName.startsWith(rootRecordname + ".")) {
      itemName = itemName.substring((rootRecordname + ".").length());
    }

    Map<String, String> labelNameMap =
        Arrays.asList(htmlItems).stream().collect(Collectors.toMap(e -> e.getId(),
            e -> e.getDisplayNameId() == null ? e.getId() : e.getDisplayNameId()));
    String labelItemName = labelNameMap.get(itemName);

    // htmlItems上で定義がない場合 /
    // htmlItems上でlabelItemNameが未定義の場合はlabelItemNameがnullになるが、その場合はもとのfieldNameの値を入れておく
    labelItemName = labelItemName == null ? itemName : labelItemName;

    // htmlItems上、関連を使用している場合はaccGroup.nameのようにfieldName自体にentityNameを含む場合があるので考慮
    return labelItemName.contains(".") ? labelItemName : rootRecordname + "." + labelItemName;
  }

  /**
   * htmlItemsについて、個別機能のlistと共通のlistをmergeさせるために使用する。
   * あくまでutilレベルなので個別処理にしても良いのだが、極力個別コードを減らしたいので本クラスに保持する。
   */
  default HtmlField[] mergeHtmlItems(HtmlField[] items1, HtmlField[] items2) {
    List<HtmlField> list = new ArrayList<>(Arrays.asList(items1));

    // common側と個別側で同一項目が定義されている場合はエラーとする
    List<String> item1Keys = Arrays.asList(items1).stream().map(e -> e.getId()).toList();

    for (String item2Key : Arrays.asList(items2).stream().map(e -> e.getId()).toList()) {
      if (item1Keys.contains(item2Key)) {
        throw new RuntimeException(
            "'itemName' of HtmlItem[] duplicated with commonHtmlItems. key: " + item2Key);
      }
    }

    list.addAll(Arrays.asList(items2));

    return list.toArray(new HtmlField[list.size()]);

  }


  default List<String> getNotEmptyFields(String loginState, RolesAndAuthoritiesBean bean) {
    List<String> list = new ArrayList<>();

    HtmlField[] htmlItems = getHtmlFields();
    for (HtmlField item : htmlItems) {
      if (item.getIsNotEmpty(loginState, bean)) {
        list.add(item.getId());
      }
    }

    return list;
  }

  default boolean isNotEmpty(String fieldName, String loginState, String rolesOrAuthoritiesString) {
    SplibSecurityUtil.RolesAndAuthoritiesBean bean =
        new SplibSecurityUtil().getRolesAndAuthoritiesBean(rolesOrAuthoritiesString);
    return getNotEmptyFields(loginState, bean).contains(fieldName);
  }
}
