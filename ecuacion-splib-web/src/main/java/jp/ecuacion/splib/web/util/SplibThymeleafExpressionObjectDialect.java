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

import org.springframework.stereotype.Component;
import org.thymeleaf.dialect.AbstractDialect;
import org.thymeleaf.dialect.IExpressionObjectDialect;
import org.thymeleaf.expression.IExpressionObjectFactory;

/**
 * Registers {@link SplibThymeleafExpressionObjectFactory} so its expression objects (see that
 * class's javadoc) become available as {@code #compUtil}/etc.
 *
 * <p>This is a plain {@code @Component}, not a {@code @Configuration}-declared {@code IDialect}
 *     bean, so Spring Boot's {@code ThymeleafAutoConfiguration} (which collects every {@code
 *     IDialect} bean in the context and registers it on the {@code SpringTemplateEngine}) picks it
 *     up automatically in any application that component-scans this package - no extra wiring
 *     needed downstream.</p>
 */
@Component
public class SplibThymeleafExpressionObjectDialect extends AbstractDialect
    implements IExpressionObjectDialect {

  private final SplibThymeleafExpressionObjectFactory factory;

  /**
   * Constructs a new instance.
   */
  public SplibThymeleafExpressionObjectDialect(SplibThymeleafExpressionObjectFactory factory) {
    super("Splib Expression Objects");
    this.factory = factory;
  }

  @Override
  public IExpressionObjectFactory getExpressionObjectFactory() {
    return factory;
  }
}
