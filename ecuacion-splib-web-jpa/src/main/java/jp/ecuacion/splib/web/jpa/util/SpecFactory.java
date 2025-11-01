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
package jp.ecuacion.splib.web.jpa.util;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jp.ecuacion.lib.jpa.entity.EclibEntity;
import jp.ecuacion.splib.core.record.SplibRecord;
import jp.ecuacion.splib.web.item.SplibWebItemContainer;
import jp.ecuacion.splib.web.record.StringMatchingConditionBean;
import jp.ecuacion.splib.web.record.StringMatchingConditionBean.StringMatchingPatternEnum;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

/**
 * Provides Spec factory.
 * 
 * @param <T> Entity
 */
public class SpecFactory<T extends EclibEntity> {

  /**
   * String、boolean、数値など、同一比較であれば型を絞らず共通使用可能。Enum項目（DB上はString）はvalueをEnum指定する必要があるので注意.
   * fieldは"name", "parentRecord.id", "parentRecord.childRecord.id"など関連のrecordを含めて記載可能。
   */
  @SuppressWarnings("unused")
  public Specification<T> equals(String field, Object value) {

    return (root, query, cb) -> {
      // entityの"."の区切り分ループ
      String[] entities = field.split("\\.");
      Path<?> path = root;
      for (int i = 0; i < entities.length; i++) {
        path = path.get(entities[i]);
      }

      return (value == null || (value instanceof String && ((String) value).equals("")) ? null
          : cb.equal(path, value));
    };
  }

  /**
   * Returns Specification for stringEqualsIgnoringCase.
   * 
   * @param field field
   * @param value value
   * @return {@code Specification<T>}
   */
  public Specification<T> stringEqualsIgnoringCase(String field, String value) {
    return stringEqualsIgnoringCase(null, field, value);
  }

  /**
   * Returns Specification for stringEqualsIgnoringCase.
   * 
   * @param field field
   * @param value value
   * @return {@code Specification<T>}
   */
  @SuppressWarnings("unused")
  public Specification<T> stringEqualsIgnoringCase(String entity, String field, String value) {
    return (root, query, cb) -> {
      Expression<String> criteriaField =
          entity == null ? root.get(field) : root.get(entity).get(field);
      return StringUtils.isEmpty(value) ? null
          : cb.equal(cb.upper(criteriaField), value.toUpperCase());
    };
  }

  /**
   * Returns Specification for stringEquals.
   * 
   * @param field field
   * @param value value
   * @return {@code Specification<T>}
   */
  public Specification<T> stringEquals(String field, String value, boolean ignoresCase) {
    return stringEquals(null, field, value, ignoresCase);
  }

  /**
   * Returns Specification for stringEquals.
   * 
   * @param field field
   * @param value value
   * @return {@code Specification<T>}
   */
  @SuppressWarnings("unused")
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

  /**
   * Returns Specification for stringNotEquals.
   * 
   * @param field field
   * @param value value
   * @return {@code Specification<T>}
   */
  public Specification<T> stringNotEquals(String field, String value) {
    return stringNotEquals(null, field, value);
  }

  /**
   * Returns Specification for stringNotEquals.
   * 
   * @param field field
   * @param value value
   * @return {@code Specification<T>}
   */
  @SuppressWarnings("unused")
  public Specification<T> stringNotEquals(String entity, String field, String value) {
    return (root, query, cb) -> {
      Expression<String> criteriaField =
          entity == null ? root.get(field) : root.get(entity).get(field);
      String criteriaValue = value;

      return StringUtils.isEmpty(value) ? null : cb.notEqual(criteriaField, criteriaValue);
    };
  }

  /**
   * Returns Specification for stringContains.
   * 
   * @param field field
   * @param value value
   * @return {@code Specification<T>}
   */
  public Specification<T> stringContains(String field, String value) {
    return stringContains(null, field, value, false);
  }

  /**
   * Returns Specification for stringContains.
   * 
   * @param field field
   * @param value value
   * @return {@code Specification<T>}
   */
  public Specification<T> stringContains(String entity, String field, String value) {
    return stringContains(entity, field, value, false);
  }

  private Specification<T> stringContains(String entity, String field, String value,
      boolean ignoresCase) {
    return stringSearchPattern(entity, field, value, "%" + value + "%", ignoresCase);
  }

  /**
   * Returns Specification for stringContainsIgnoringCase.
   * 
   * @param field field
   * @param value value
   * @return {@code Specification<T>}
   */
  public Specification<T> stringContainsIgnoringCase(String field, String value) {
    return stringContains(null, field, value, true);
  }

  /**
   * Returns Specification for stringContainsIgnoringCase.
   * 
   * @param field field
   * @param value value
   * @return {@code Specification<T>}
   */
  public Specification<T> stringContainsIgnoringCase(String entity, String field, String value) {
    return stringContains(entity, field, value, true);
  }

  /**
   * Returns Specification for stringStartsWith.
   * 
   * @param field field
   * @param value value
   * @return {@code Specification<T>}
   */
  public Specification<T> stringStartsWith(String field, String value) {
    return stringStartsWith(null, field, value, false);
  }

  /**
   * Returns Specification for stringStartsWith.
   * 
   * @param field field
   * @param value value
   * @return {@code Specification<T>}
   */
  public Specification<T> stringStartsWith(String entity, String field, String value) {
    return stringStartsWith(entity, field, value, false);
  }

  /**
   * Returns Specification for stringStartsWith.
   * 
   * @param field field
   * @param value value
   * @return {@code Specification<T>}
   */
  private Specification<T> stringStartsWith(String entity, String field, String value,
      boolean ignoresCase) {
    return stringSearchPattern(entity, field, value, value + "%", ignoresCase);
  }

  /**
   * Returns Specification for stringStartsWithIgnoringCase.
   * 
   * @param field field
   * @param value value
   * @return {@code Specification<T>}
   */
  public Specification<T> stringStartsWithIgnoringCase(String field, String value) {
    return stringStartsWith(null, field, value, true);
  }

  /**
   * Returns Specification for stringStartsWithIgnoringCase.
   * 
   * @param field field
   * @param value value
   * @return {@code Specification<T>}
   */
  public Specification<T> stringStartsWithIgnoringCase(String entity, String field, String value) {
    return stringStartsWith(entity, field, value, true);
  }

  /**
   * Returns Specification for stringStartsWithIgnoringCase.
   * 
   * @param field field
   * @param value value
   * @return {@code Specification<T>}
   */
  public Specification<T> stringEndsWith(String field, String value) {
    return stringEndsWith(null, field, value, false);
  }

  /**
   * Returns Specification for stringEndsWith.
   * 
   * @param field field
   * @param value value
   * @return {@code Specification<T>}
   */
  public Specification<T> stringEndsWith(String entity, String field, String value) {
    return stringEndsWith(entity, field, value, false);
  }

  /**
   * Returns Specification for stringEndsWith.
   * 
   * @param field field
   * @param value value
   * @return {@code Specification<T>}
   */
  private Specification<T> stringEndsWith(String entity, String field, String value,
      boolean ignoresCase) {
    return stringSearchPattern(entity, field, value, "%" + value, ignoresCase);
  }

  /**
   * Returns Specification for stringEndsWithIgnoringCase.
   * 
   * @param field field
   * @param value value
   * @return {@code Specification<T>}
   */
  public Specification<T> stringEndsWithIgnoringCase(String field, String value) {
    return stringEndsWith(null, field, value, true);
  }

  /**
   * Returns Specification for stringEndsWithIgnoringCase.
   * 
   * @param field field
   * @param value value
   * @return {@code Specification<T>}
   */
  public Specification<T> stringEndsWithIgnoringCase(String entity, String field, String value) {
    return stringEndsWith(entity, field, value, true);
  }

  @SuppressWarnings("unused")
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

  /**
   * Returns Specification for localDateEqualToOrLessThan.
   * 
   * @param field field
   * @param date date
   * @return {@code Specification<T>}
   */
  public Specification<T> localDateEqualToOrLessThan(String field, LocalDate date) {
    return localDateEqualToOrLessThan(null, field, date);
  }

  /**
   * Returns Specification for localDateEqualToOrLessThan.
   * 
   * @param field field
   * @param date date
   * @return {@code Specification<T>}
   */
  @SuppressWarnings("unused")
  public Specification<T> localDateEqualToOrLessThan(String entity, String field, LocalDate date) {
    return date == null ? null : (root, query, cb) -> {
      Expression<LocalDate> criteriaField =
          entity == null ? root.get(field) : root.get(entity).get(field);
      return cb.or(cb.equal(criteriaField.as(LocalDate.class), date),
          cb.lessThan(criteriaField.as(LocalDate.class), date));
    };
  }

  /**
   * Returns Specification for localDateEqualToOrLessThan.
   * 
   * @param field field
   * @param date date
   * @return {@code Specification<T>}
   */
  @SuppressWarnings("unused")
  public Specification<T> localDateEqualToOrGreaterThan(String field, LocalDate date) {
    return date == null ? null : (root, query, cb) -> {
      return cb.or(cb.equal(root.get(field).as(LocalDate.class), date),
          cb.greaterThan(root.get(field).as(LocalDate.class), date));
    };
  }

  /**
   * Returns Specification for localDateEqualToOrGreaterThan.
   * 
   * @param field field
   * @param date date
   * @return {@code Specification<T>}
   */
  @SuppressWarnings("unused")
  public Specification<T> localDateEqualToOrGreaterThan(String entity, String field,
      LocalDate date) {
    return date == null ? null : (root, query, cb) -> {
      Expression<LocalDate> criteriaField =
          entity == null ? root.get(field) : root.get(entity).get(field);
      return cb.or(cb.equal(criteriaField.as(LocalDate.class), date),
          cb.greaterThan(criteriaField.as(LocalDate.class), date));
    };
  }

  /**
   * add all the search conditions with string values.
   * 
   * @param rec rec
   * @return {@code List<Specification<T>>}
   */
  public List<Specification<T>> addStringSearchConditions(SplibRecord rec) {
    List<Specification<T>> list = new ArrayList<>();

    for (Map.Entry<String, StringMatchingConditionBean> entry : ((SplibWebItemContainer) rec)
        .getSearchPatterns().entrySet()) {
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
