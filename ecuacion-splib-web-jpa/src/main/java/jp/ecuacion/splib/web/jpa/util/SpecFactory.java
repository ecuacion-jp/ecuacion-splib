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
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import jp.ecuacion.splib.core.record.SplibRecord;
import jp.ecuacion.splib.jpa.entity.SplibEntity;
import jp.ecuacion.splib.web.bean.StringMatchingConditionBean.StringMatchingPatternEnum;
import jp.ecuacion.splib.web.item.HtmlItem;
import jp.ecuacion.splib.web.item.HtmlItemContainer;
import jp.ecuacion.splib.web.item.HtmlItemSelect;
import jp.ecuacion.splib.web.item.HtmlItemString;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

/**
 * Provides Spec factory.
 * 
 * @param <T> Entity
 */
public class SpecFactory<T extends SplibEntity> {

  /**
   * Usable generically for String, boolean, numeric types, etc. when doing equality comparison.
   * Note: Enum fields (stored as String in DB) require the value to be specified as an Enum.
   * The field can include related records, e.g. "name", "parentRecord.id",
   * "parentRecord.childRecord.id".
   */
  public @Nullable Specification<T> equals(String propertyPath, @Nullable Object value) {

    return (root, query, cb) -> {
      Path<?> path = getPath(root, propertyPath, value);

      return (value == null || (value instanceof String && ((String) value).isEmpty()) ? null
          : cb.equal(path, value));
    };
  }

  /**
   * Returns Specification for stringEqualsIgnoringCase.
   * 
   * @param propertyPath propertyPath
   * @param value value
   * @return {@code Specification<T>}
   */
  @SuppressWarnings("null")
  public Specification<T> stringEqualsIgnoringCase(String propertyPath, String value) {
    return (root, query, cb) -> {
      Expression<String> criteriaField = getPath(root, propertyPath, value);
      return StringUtils.isEmpty(value) ? null
          : cb.equal(cb.upper(criteriaField), value.toUpperCase());
    };
  }

  /**
   * Returns Specification for stringEquals.
   * 
   * @param propertyPath propertyPath
   * @param value value
   * @return {@code Specification<T>}
   */
  public @Nullable Specification<T> stringEquals(String propertyPath, @Nullable String value,
      boolean ignoresCase) {
    return (root, query, cb) -> {
      Expression<String> criteriaFieldTmp = getPath(root, propertyPath, value);
      Expression<String> criteriaField =
          ignoresCase ? cb.upper(criteriaFieldTmp) : criteriaFieldTmp;
      String criteriaValue = ignoresCase
          ? (value == null ? "" : value.toUpperCase()) : (value == null ? "" : value);

      return StringUtils.isEmpty(value) ? null : cb.equal(criteriaField, criteriaValue);
    };
  }

  /**
   * Returns Specification for stringNotEquals.
   * 
   * @param propertyPath propertyPath
   * @param value value
   * @return {@code Specification<T>}
   */
  @SuppressWarnings("null")
  public Specification<T> stringNotEquals(String propertyPath, String value) {
    return (root, query, cb) -> {
      Expression<String> criteriaField = getPath(root, propertyPath, value);
      String criteriaValue = value;

      return StringUtils.isEmpty(value) ? null : cb.notEqual(criteriaField, criteriaValue);
    };
  }

  /**
   * Returns Specification for stringContains.
   * 
   * @param propertyPath propertyPath
   * @param value value
   * @return {@code Specification<T>}
   */
  public @Nullable Specification<T> stringContains(String propertyPath, @Nullable String value) {
    return stringContains(propertyPath, value, false);
  }

  private @Nullable Specification<T> stringContains(String propertyPath, @Nullable String value,
      boolean ignoresCase) {
    return value == null ? null
        : stringSearchPattern(propertyPath, value, "%" + value + "%", ignoresCase);
  }

  /**
   * Returns Specification for stringContainsIgnoringCase.
   * 
   * @param propertyPath propertyPath
   * @param value value
   * @return {@code Specification<T>}
   */
  public @Nullable Specification<T> stringContainsIgnoringCase(String propertyPath,
      @Nullable String value) {
    return stringContains(propertyPath, value, true);
  }

  /**
   * Returns Specification for stringStartsWith.
   * 
   * @param propertyPath propertyPath
   * @param value value
   * @return {@code Specification<T>}
   */
  public @Nullable Specification<T> stringStartsWith(String propertyPath, @Nullable String value) {
    return stringStartsWith(propertyPath, value, false);
  }

  /**
   * Returns Specification for stringStartsWith.
   * 
   * @param propertyPath propertyPath
   * @param value value
   * @return {@code Specification<T>}
   */
  private @Nullable Specification<T> stringStartsWith(String propertyPath, @Nullable String value,
      boolean ignoresCase) {
    return value == null ? null
        : stringSearchPattern(propertyPath, value, value + "%", ignoresCase);
  }

  /**
   * Returns Specification for stringStartsWithIgnoringCase.
   * 
   * @param propertyPath propertyPath
   * @param value value
   * @return {@code Specification<T>}
   */
  public @Nullable Specification<T> stringStartsWithIgnoringCase(String propertyPath,
      @Nullable String value) {
    return stringStartsWith(propertyPath, value, true);
  }

  /**
   * Returns Specification for stringEndsWith.
   * 
   * @param propertyPath propertyPath
   * @param value value
   * @return {@code Specification<T>}
   */
  public @Nullable Specification<T> stringEndsWith(String propertyPath, @Nullable String value) {
    return stringEndsWith(propertyPath, value, false);
  }

  /**
   * Returns Specification for stringEndsWith.
   * 
   * @param field field
   * @param value value
   * @return {@code Specification<T>}
   */
  private @Nullable Specification<T> stringEndsWith(String propertyPath, @Nullable String value,
      boolean ignoresCase) {
    return value == null ? null
        : stringSearchPattern(propertyPath, value, "%" + value, ignoresCase);
  }

  /**
   * Returns Specification for stringEndsWithIgnoringCase.
   * 
   * @param propertyPath propertyPath
   * @param value value
   * @return {@code Specification<T>}
   */
  public @Nullable Specification<T> stringEndsWithIgnoringCase(String propertyPath,
      @Nullable String value) {
    return stringEndsWith(propertyPath, value, true);
  }

  private @Nullable Specification<T> stringSearchPattern(String propertyPath,
      @Nullable String value, @Nullable String criteriaValue, boolean ignoresCase) {
    return (root, query, cb) -> {
      if (StringUtils.isEmpty(value)) {
        return null;
      }

      Expression<String> criteriaFieldTmp = getPath(root, propertyPath, value);
      Expression<String> criteriaField =
          ignoresCase ? cb.upper(criteriaFieldTmp) : criteriaFieldTmp;
      String finalCriteriaValue = ignoresCase
          ? (criteriaValue == null ? "" : criteriaValue.toUpperCase())
          : (criteriaValue == null ? "" : criteriaValue);

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
  public @Nullable Specification<T> localDateEqualToOrLessThan(String field, LocalDate date) {
    return localDateEqualToOrLessThan(null, field, date);
  }

  /**
   * Returns Specification for localDateEqualToOrLessThan.
   * 
   * @param field field
   * @param date date
   * @return {@code Specification<T>}
   */
  public @Nullable Specification<T> localDateEqualToOrLessThan(@Nullable String entity,
      String field, LocalDate date) {
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
  public @Nullable Specification<T> localDateEqualToOrGreaterThan(String field, LocalDate date) {
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
  public @Nullable Specification<T> localDateEqualToOrGreaterThan(@Nullable String entity,
      String field, LocalDate date) {
    return date == null ? null : (root, query, cb) -> {
      Expression<LocalDate> criteriaField =
          entity == null ? root.get(field) : root.get(entity).get(field);
      return cb.or(cb.equal(criteriaField.as(LocalDate.class), date),
          cb.greaterThan(criteriaField.as(LocalDate.class), date));
    };
  }

  @SuppressWarnings({"unused", "null"})
  private Specification<T> selectSearchPattern(String propertyPath, String value,
      String criteriaValue, boolean ignoresCase) {
    return (root, query, cb) -> {
      if (StringUtils.isEmpty(value)) {
        return null;
      }

      Path<String> criteriaFieldTmp = getPath(root, propertyPath, value);
      Expression<String> criteriaField =
          ignoresCase ? cb.upper(criteriaFieldTmp) : criteriaFieldTmp;
      String finalCriteriaValue = ignoresCase
          ? (criteriaValue == null ? "" : criteriaValue.toUpperCase())
          : (criteriaValue == null ? "" : criteriaValue);

      return cb.like((criteriaField), finalCriteriaValue);
    };
  }

  private <X> @Nullable Path<X> getPath(Root<T> root, String propertyPath, @Nullable X value) {
    // Loop the number of "." in propertyPath
    String[] fields = propertyPath.split("\\.");
    Path<X> path = null;
    for (int i = 0; i < fields.length; i++) {
      if (path == null) {
        path = root.get(fields[i]);
      } else {
        path = path.get(fields[i]);
      }
    }

    return path;
  }

  /**
   * add all the search conditions which are sure how to search from the kind of HtmlItem.
   * 
   * @param rec rec
   * @return {@code List<Specification<T>>}
   */
  public List<Specification<T>> addExplicitSearchConditions(SplibRecord rec) {
    List<Specification<T>> list = new ArrayList<>();

    for (HtmlItem item : ((HtmlItemContainer) rec).getHtmlItems()) {
      String propertyPath = item.getPropertyPath();
      Object value = null;

      try {
        value = (Object) rec.getValue(propertyPath);
      } catch (RuntimeException ex) {
        if (ex.getCause() instanceof NoSuchMethodException) {
          // items with unexistent itemPropertyPath is allowed.
          // no exception thrown and skip the rest.
          continue;

        } else {
          throw ex;
        }
      }

      if (item instanceof HtmlItemString) {
        HtmlItemString stringItem = ((HtmlItemString) item);

        Specification<T> spec = null;
        if (stringItem.getStringSearchPatternEnum() == StringMatchingPatternEnum.EXACT) {
          spec = stringEquals(propertyPath, (String) value, stringItem.isIgnoresCase());

        } else if (stringItem.getStringSearchPatternEnum() == StringMatchingPatternEnum.PARTIAL) {
          spec = stringContains(propertyPath, (String) value, stringItem.isIgnoresCase());

        } else if (stringItem.getStringSearchPatternEnum() == StringMatchingPatternEnum.PREFIX) {
          spec = stringStartsWith(propertyPath, (String) value, stringItem.isIgnoresCase());

        } else if (stringItem.getStringSearchPatternEnum() == StringMatchingPatternEnum.POSTFIX) {
          spec = stringEndsWith(propertyPath, (String) value, stringItem.isIgnoresCase());
        }
        if (spec != null) {
          list.add(spec);
        }

      } else if (item instanceof HtmlItemSelect) {
        @Nullable Specification<T> eqSpec = equals(propertyPath, value);
        if (eqSpec != null) {
          list.add(eqSpec);
        }
      }
    }

    return list;
  }
}
