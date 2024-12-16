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
package jp.ecuacion.splib.web.util;

import jakarta.persistence.criteria.Expression;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jp.ecuacion.lib.jpa.entity.AbstractEntity;
import jp.ecuacion.splib.core.form.record.SplibRecord;
import jp.ecuacion.splib.web.form.record.SearchRecordInterface;
import jp.ecuacion.splib.web.form.record.StringMatchingConditionBean;
import jp.ecuacion.splib.web.form.record.StringMatchingConditionBean.StringMatchingPatternEnum;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

public class SpecFactory<T extends AbstractEntity> {

  /**
   * String、boolean、数値など、同一比較であれば型を絞らず共通使用可能。 Enum項目（DB上はString）はvalueをEnum指定する必要があるので注意。
   * 関連を使用していない項目に対して使用。
   */
  public Specification<T> equals(String field, Object value) {
    return equals(null, field, value);
  }

  /**
   * String、boolean、数値など、同一比較であれば型を絞らず共通使用可能。Enum項目（DB上はString）はvalueをEnum指定する必要があるので注意。
   * entityは通常はnull、関連を使用しておりentity内のfieldとして保持している項目に対して、そのfieldにもつentity classに対しては値を指定。
   * その際、引数のentityは"accGroup"のように指定。
   */
  public Specification<T> equals(String entity, String field, Object value) {
    return (root, query, cb) -> {
      Expression<?> criteriaField = entity == null ? root.get(field) : root.get(entity).get(field);
      return (value == null || (value instanceof String && ((String) value).equals("")) ? null
          : cb.equal(criteriaField, value));
    };
  }

  public Specification<T> stringEqualsIgnoringCase(String field, String value) {
    return stringEqualsIgnoringCase(null, field, value);
  }

  public Specification<T> stringEqualsIgnoringCase(String entity, String field, String value) {
    return (root, query, cb) -> {
      Expression<String> criteriaField =
          entity == null ? root.get(field) : root.get(entity).get(field);
      return StringUtils.isEmpty(value) ? null
          : cb.equal(cb.upper(criteriaField), value.toUpperCase());
    };
  }

  public Specification<T> stringEquals(String field, String value, boolean ignoresCase) {
    return stringEquals(null, field, value, ignoresCase);
  }

  public Specification<T> stringEquals(String entity, String field, String value,
      boolean ignoresCase) {
    return (root, query, cb) -> {
      Expression<String> criteriaFieldTmp =
          entity == null ? root.get(field) : root.get(entity).get(field);
      Expression<String> criteriaField =
          ignoresCase ? cb.upper(criteriaFieldTmp) : criteriaFieldTmp;
      String criteriaValue = ignoresCase ? value.toUpperCase() : value;

      return StringUtils.isEmpty(value) ? null : cb.equal(criteriaField, criteriaValue);
    };
  }

  public Specification<T> stringNotEquals(String field, String value) {
    return stringNotEquals(null, field, value);
  }

  public Specification<T> stringNotEquals(String entity, String field, String value) {
    return (root, query, cb) -> {
      Expression<String> criteriaField =
          entity == null ? root.get(field) : root.get(entity).get(field);
      String criteriaValue = value;

      return StringUtils.isEmpty(value) ? null : cb.notEqual(criteriaField, criteriaValue);
    };
  }

  public Specification<T> stringContains(String field, String value) {
    return stringContains(null, field, value, false);
  }

  public Specification<T> stringContains(String entity, String field, String value) {
    return stringContains(entity, field, value, false);
  }

  private Specification<T> stringContains(String entity, String field, String value,
      boolean ignoresCase) {
    return stringSearchPattern(entity, field, value, "%" + value + "%", ignoresCase);
  }

  public Specification<T> stringContainsIgnoringCase(String field, String value) {
    return stringContains(null, field, value, true);
  }

  public Specification<T> stringContainsIgnoringCase(String entity, String field, String value) {
    return stringContains(entity, field, value, true);
  }

  public Specification<T> stringStartsWith(String field, String value) {
    return stringStartsWith(null, field, value, false);
  }

  public Specification<T> stringStartsWith(String entity, String field, String value) {
    return stringStartsWith(entity, field, value, false);
  }

  private Specification<T> stringStartsWith(String entity, String field, String value,
      boolean ignoresCase) {
    return stringSearchPattern(entity, field, value, value + "%", ignoresCase);
  }

  public Specification<T> stringStartsWithIgnoringCase(String field, String value) {
    return stringStartsWith(null, field, value, true);
  }

  public Specification<T> stringStartsWithIgnoringCase(String entity, String field, String value) {
    return stringStartsWith(entity, field, value, true);
  }

  public Specification<T> stringEndsWith(String field, String value) {
    return stringEndsWith(null, field, value, false);
  }

  public Specification<T> stringEndsWith(String entity, String field, String value) {
    return stringEndsWith(entity, field, value, false);
  }

  private Specification<T> stringEndsWith(String entity, String field, String value,
      boolean ignoresCase) {
    return stringSearchPattern(entity, field, value, "%" + value, ignoresCase);
  }

  public Specification<T> stringEndsWithIgnoringCase(String field, String value) {
    return stringEndsWith(null, field, value, true);
  }

  public Specification<T> stringEndsWithIgnoringCase(String entity, String field, String value) {
    return stringEndsWith(entity, field, value, true);
  }

  private Specification<T> stringSearchPattern(String entity, String field, String value,
      String criteriaValue, boolean ignoresCase) {
    return (root, query, cb) -> {
      if (StringUtils.isEmpty(value)) {
        return null;
      }

      Expression<String> criteriaFieldTmp =
          entity == null ? root.get(field) : root.get(entity).get(field);
      Expression<String> criteriaField =
          ignoresCase ? cb.upper(criteriaFieldTmp) : criteriaFieldTmp;
      String finalCriteriaValue = ignoresCase ? criteriaValue.toUpperCase() : criteriaValue;

      return cb.like((criteriaField), finalCriteriaValue);
    };
  }

  public Specification<T> localDateEqualToOrLessThan(String field, LocalDate date) {
    return localDateEqualToOrLessThan(null, field, date);
  }

  public Specification<T> localDateEqualToOrLessThan(String entity, String field, LocalDate date) {
    return date == null ? null : (root, query, cb) -> {
      Expression<LocalDate> criteriaField =
          entity == null ? root.get(field) : root.get(entity).get(field);
      return cb.or(cb.equal(criteriaField.as(LocalDate.class), date),
          cb.lessThan(criteriaField.as(LocalDate.class), date));
    };
  }

  public Specification<T> localDateEqualToOrGreaterThan(String field, LocalDate date) {
    return date == null ? null : (root, query, cb) -> {
      return cb.or(cb.equal(root.get(field).as(LocalDate.class), date),
          cb.greaterThan(root.get(field).as(LocalDate.class), date));
    };
  }

  public Specification<T> localDateEqualToOrGreaterThan(String entity, String field,
      LocalDate date) {
    return date == null ? null : (root, query, cb) -> {
      Expression<LocalDate> criteriaField =
          entity == null ? root.get(field) : root.get(entity).get(field);
      return cb.or(cb.equal(criteriaField.as(LocalDate.class), date),
          cb.greaterThan(criteriaField.as(LocalDate.class), date));
    };
  }

  public List<Specification<T>> addStringSearchConditions(SplibRecord rec) {
    List<Specification<T>> list = new ArrayList<>();

    for (Map.Entry<String, StringMatchingConditionBean> entry 
        : ((SearchRecordInterface) rec).getSearchPatterns().entrySet()) {
      String key = entry.getKey();
      String entity = key.contains(".") ? key.substring(0, key.indexOf(".")) : null;
      String field = key.contains(".") ? key.substring(key.indexOf(".") + 1) : key;

      if (entry.getValue().getStringSearchPatternEnum() == StringMatchingPatternEnum.EXACT) {
        list.add(stringEquals(entity, field, (String) rec.getValue(entry.getKey()),
            entry.getValue().isIgnoresCase()));

      } else if (entry.getValue()
          .getStringSearchPatternEnum() == StringMatchingPatternEnum.PARTIAL) {
        list.add(stringContains(entity, field, (String) rec.getValue(entry.getKey()),
            entry.getValue().isIgnoresCase()));

      } else if (entry.getValue()
          .getStringSearchPatternEnum() == StringMatchingPatternEnum.PREFIX) {
        list.add(stringStartsWith(entity, field, (String) rec.getValue(entry.getKey()),
            entry.getValue().isIgnoresCase()));

      } else if (entry.getValue()
          .getStringSearchPatternEnum() == StringMatchingPatternEnum.POSTFIX) {
        list.add(stringEndsWith(entity, field, (String) rec.getValue(entry.getKey()),
            entry.getValue().isIgnoresCase()));
      }
    }

    return list;
  }
}
