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

/**
 * Stores string match condition for search function.
 */
public class StringMatchingConditionBean {

  /** Stores the search pattern of a string. */
  private StringMatchingPatternEnum stringSearchPatternEnum;

  /** Stores whether cases of a string is distinguished. */
  private boolean ignoresCase;

  /**
   * Constructs a new instance.
   */
  public StringMatchingConditionBean() {
    this.stringSearchPatternEnum = StringMatchingPatternEnum.PARTIAL;
    this.ignoresCase = true;
  }

  /**
   * Constructs a new instance.
   */
  public StringMatchingConditionBean(StringMatchingPatternEnum stringSearchPatternEnum) {
    this.stringSearchPatternEnum = stringSearchPatternEnum;
    this.ignoresCase = true;
  }

  /**
   * Constructs a new instance.
   */
  public StringMatchingConditionBean(StringMatchingPatternEnum stringSearchPatternEnum,
      boolean ignoresCase) {
    this.stringSearchPatternEnum = stringSearchPatternEnum;
    this.ignoresCase = ignoresCase;
  }

  public StringMatchingPatternEnum getStringSearchPatternEnum() {
    return stringSearchPatternEnum;
  }

  public boolean isIgnoresCase() {
    return ignoresCase;
  }

  /**
   * Has string match patterns.
   */
  public static enum StringMatchingPatternEnum {
    EXACT, PARTIAL, PREFIX, POSTFIX
  }
}
