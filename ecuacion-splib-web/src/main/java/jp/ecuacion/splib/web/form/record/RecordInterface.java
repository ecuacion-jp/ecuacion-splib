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
import jp.ecuacion.splib.web.util.SplibSecurityUtil;
import jp.ecuacion.splib.web.util.SplibSecurityUtil.RolesAndAuthoritiesBean;

/**
 * Has features related web environment.
 */
public interface RecordInterface {

  /**
   * Returns {@code HtmlItem} from {@code HtmlItem[]} and {@code fieldId}. 
   * 
   * @param itemPropertyPath itemPropertyPath
   * @return HtmlItem
   */
  default HtmlItem getHtmlItem(String rootRecordName, String itemPropertyPath) {
    Map<String, HtmlItem> map = Arrays.asList(getHtmlItems()).stream()
        .collect(Collectors.toMap(e -> e.getItemPropertyPath(), e -> e));

    HtmlItem field = map.get(itemPropertyPath);

    // Try with itemPropertyPath
    if (field == null && itemPropertyPath.startsWith(rootRecordName)) {
      field = map.get(itemPropertyPath.substring(rootRecordName.length() + 1));
    }

    return field == null ? new HtmlItem(itemPropertyPath) : field;
  }

  /**
   * Returns HtmlItem.
   * 
   * @return HtmlItem[]
   */
  HtmlItem[] getHtmlItems();

  /**
  * Returns whether the itemKindId needs comma.
  *
  * @param propertyPath propertyPath
  * @return boolean
  */
  default boolean needsCommas(String rootRecordName, String propertyPath) {
    HtmlItem item = getHtmlItem(rootRecordName, propertyPath);

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
  default String getDisplayName(String rootRecordName, String propertyPathWithoutRootRecordName) {
    HtmlItem htmlItem = getHtmlItem(rootRecordName, propertyPathWithoutRootRecordName);
    String displayNameId = htmlItem.getItemNameKey(rootRecordName);

    // htmlItems上、関連を使用している場合はaccGroup.nameのようにfieldName自体にentityNameを含む場合があるので考慮
    return displayNameId.contains(".") ? displayNameId
        : propertyPathWithoutRootRecordName + "." + displayNameId;
  }

  /**
   * htmlItemsについて、個別機能のlistと共通のlistをmergeさせるために使用する.
   * あくまでutilレベルなので個別処理にしても良いのだが、極力個別コードを減らしたいので本クラスに保持する。
   */
  default HtmlItem[] mergeHtmlItems(HtmlItem[] fields1, HtmlItem[] fields2) {
    List<HtmlItem> list = new ArrayList<>(Arrays.asList(fields1));

    // common側と個別側で同一項目が定義されている場合はエラーとする
    List<String> propertyPath1List =
        Arrays.asList(fields1).stream().map(e -> e.getItemPropertyPath()).toList();

    for (String propertyPath2 : Arrays.asList(fields2).stream().map(e -> e.getItemPropertyPath())
        .toList()) {
      if (propertyPath1List.contains(propertyPath2)) {
        throw new RuntimeException(
            "'id' of HtmlItem[] duplicated with commonHtmlItems. key: " + propertyPath2);
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
  @Deprecated
  default List<String> getNotEmptyFields(String loginState, RolesAndAuthoritiesBean bean) {
    return Arrays.asList(getHtmlItems()).stream()
        .filter(item -> item.getIsNotEmpty(loginState, bean))
        .map(item -> item.getItemPropertyPath()).toList();
  }

  /**
   * Returns whether isNotEmpty or not.
   * 
   * @param fieldId fieldId
   * @param loginState loginState
   * @param rolesOrAuthoritiesString rolesOrAuthoritiesString
   * @return boolean
   */
  @Deprecated
  default boolean isNotEmpty(String fieldId, String loginState, String rolesOrAuthoritiesString) {
    SplibSecurityUtil.RolesAndAuthoritiesBean bean =
        new SplibSecurityUtil().getRolesAndAuthoritiesBean(rolesOrAuthoritiesString);
    return getNotEmptyFields(loginState, bean).contains(fieldId);
  }

  /**
   * Obtrains NotEmpty fields.
   * 
   * @param loginState loginState
   * @param bean bean
   * @return {@code List<String>}
   */
  default List<String> getRequiredFields(String loginState, RolesAndAuthoritiesBean bean) {
    return Arrays.asList(getHtmlItems()).stream()
        .filter(item -> item.getIsNotEmpty(loginState, bean))
        .map(item -> item.getItemPropertyPath()).toList();
  }

  /**
   * Returns whether isNotEmpty or not.
   * 
   * @param fieldId fieldId
   * @param loginState loginState
   * @param rolesOrAuthoritiesString rolesOrAuthoritiesString
   * @return boolean
   */
  default boolean isRequired(String fieldId, String loginState, String rolesOrAuthoritiesString) {
    SplibSecurityUtil.RolesAndAuthoritiesBean bean =
        new SplibSecurityUtil().getRolesAndAuthoritiesBean(rolesOrAuthoritiesString);
    return getRequiredFields(loginState, bean).contains(fieldId);
  }

  /**
   * Obtrains NotEmpty fields.
   * 
   * @param loginState loginState
   * @param bean bean
   * @return {@code List<String>}
   */
  default List<String> getRequiredFieldsOnSearch(String loginState, RolesAndAuthoritiesBean bean) {
    return Arrays.asList(getHtmlItems()).stream()
        .filter(item -> item.getIsNotEmptyOnSearch(loginState, bean))
        .map(item -> item.getItemPropertyPath()).toList();
  }

  /**
   * Returns whether isNotEmpty or not.
   * 
   * @param fieldId fieldId
   * @param loginState loginState
   * @param rolesOrAuthoritiesString rolesOrAuthoritiesString
   * @return boolean
   */
  default boolean isRequiredOnSearch(String fieldId, String loginState,
      String rolesOrAuthoritiesString) {
    SplibSecurityUtil.RolesAndAuthoritiesBean bean =
        new SplibSecurityUtil().getRolesAndAuthoritiesBean(rolesOrAuthoritiesString);
    return getRequiredFieldsOnSearch(loginState, bean).contains(fieldId);
  }
}
