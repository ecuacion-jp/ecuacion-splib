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
package jp.ecuacion.splib.core.logging;

import ch.qos.logback.core.PropertyDefinerBase;

/**
 * A logback {@code PropertyDefiner} that fails application startup when the property named by
 * {@link #setPropertyName(String)} has not been defined elsewhere in the logback configuration.
 *
 * <p>Wire it up via {@code <define>}, passing the value through logback's own {@code ${...}}
 * substitution rather than looking it up in Java code:</p>
 *
 * <pre>{@code
 * <define name="_unused" class="jp.ecuacion.splib.core.logging.LogbackRequiredPropertyChecker">
 *     <propertyName>loglevel-root</propertyName>
 *     <propertyValue>${loglevel-root:-}</propertyValue>
 * </define>
 * }</pre>
 *
 * <p>{@code propertyValue} must be passed this way, as {@code ${name:-}}, rather than having this
 * class look the property up itself via {@code Context.getProperty(name)}: a plain
 * {@code <property name="..." value="..." />} defaults to logback's LOCAL scope, which lives only
 * in the Joran interpretation context for the current configuration file and is never copied onto
 * the {@code Context} object unless {@code scope="context"} is used explicitly. Routing the value
 * through a substituted child element instead uses the same property lookup that resolves any
 * other {@code ${...}} reference in the file, so it sees local-scope properties too.</p>
 *
 * <p>{@link #getPropertyValue()} throws rather than returning normally: logback's
 * {@code DefineModelHandler} does not catch exceptions coming from it, so the exception
 * propagates all the way out of {@code JoranConfigurator.doConfigure(...)}. Since Spring Boot
 * initializes logging before the rest of the application context, this fails application startup
 * with a clear message instead of silently falling back to logback's own default handling of an
 * unresolved {@code ${...}} reference (which, for a {@code level} attribute, is DEBUG).</p>
 */
public class LogbackRequiredPropertyChecker extends PropertyDefinerBase {

  private String propertyName;
  private String propertyValue;

  /**
   * Sets the name of the property, used only to build a readable error message.
   *
   * @param propertyName the property name
   */
  public void setPropertyName(String propertyName) {
    this.propertyName = propertyName;
  }

  /**
   * Sets the already-substituted value of the property, typically {@code ${name:-}} so it
   * resolves to an empty string (rather than the literal, unresolved {@code ${...}} text) when
   * the property is not defined.
   *
   * @param propertyValue the property's value, or blank/empty if undefined
   */
  public void setPropertyValue(String propertyValue) {
    this.propertyValue = propertyValue;
  }

  @Override
  public String getPropertyValue() {
    if (propertyValue == null || propertyValue.isBlank()) {
      throw new IllegalStateException("Required logback property [" + propertyName
          + "] is not defined. Define it (e.g. via <property name=\"" + propertyName
          + "\" value=\"...\" />) before this logback configuration file is included.");
    }

    // The <define> that invokes this checker uses a throwaway property name: this class exists
    // only for the side effect above, not for the value it returns.
    return propertyValue;
  }
}
