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

import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.expression.IExpressionObjectFactory;

/**
 * Exposes {@code compUtil}/{@code exUtil}/{@code optUtil}/{@code propUtil}/{@code strUtil}/
 * {@code themeColorUtil} as {@code #}-prefixed Thymeleaf expression objects, in addition to their
 * existing {@code @Component}-based {@code @compUtil}/etc. Spring bean access.
 *
 * <p>Thymeleaf evaluates fragment-selector parameters (and some other nested contexts) in a
 *     "restricted" SpEL mode that, since Thymeleaf 3.1.3, forbids raw {@code @beanName} SpEL bean
 *     references outright - templates using {@code @optUtil.xxx(...)} etc. in such a context fail
 *     with "Instantiation of new objects and access to static classes or parameters is forbidden
 *     in this context", even though the exact same call works fine elsewhere. {@code #}-prefixed
 *     expression objects (the same mechanism backing Thymeleaf's own {@code #strings}/{@code
 *     #messages}/etc.) are exempt from that restriction, so templates should prefer {@code
 *     #optUtil.xxx(...)} over {@code @optUtil.xxx(...)} - use the {@code @}-form only where you
 *     know for certain the expression never runs in a restricted context.</p>
 */
@Component
public class SplibThymeleafExpressionObjectFactory implements IExpressionObjectFactory {

  private static final String COMP_UTIL = "compUtil";
  private static final String EX_UTIL = "exUtil";
  private static final String OPT_UTIL = "optUtil";
  private static final String PROP_UTIL = "propUtil";
  private static final String STR_UTIL = "strUtil";
  private static final String THEME_COLOR_UTIL = "themeColorUtil";

  @SuppressWarnings("null")
  private static final Set<String> ALL_EXPRESSION_OBJECT_NAMES =
      Set.of(COMP_UTIL, EX_UTIL, OPT_UTIL, PROP_UTIL, STR_UTIL, THEME_COLOR_UTIL);

  private final SplibThymeleafComponentUtil compUtil;
  private final SplibThymeleafExceptionUtil exUtil;
  private final SplibThymeleafOptionUtil optUtil;
  private final SplibThymeleafPropertiesUtil propUtil;
  private final SplibThymeleafStringUtil strUtil;
  private final SplibThemeColorUtil themeColorUtil;

  /**
   * Constructs a new instance.
   */
  public SplibThymeleafExpressionObjectFactory(SplibThymeleafComponentUtil compUtil,
      SplibThymeleafExceptionUtil exUtil, SplibThymeleafOptionUtil optUtil,
      SplibThymeleafPropertiesUtil propUtil, SplibThymeleafStringUtil strUtil,
      SplibThemeColorUtil themeColorUtil) {
    this.compUtil = compUtil;
    this.exUtil = exUtil;
    this.optUtil = optUtil;
    this.propUtil = propUtil;
    this.strUtil = strUtil;
    this.themeColorUtil = themeColorUtil;
  }

  @Override
  public Set<String> getAllExpressionObjectNames() {
    return ALL_EXPRESSION_OBJECT_NAMES;
  }

  @SuppressWarnings("null")
  @Override
  public @Nullable Object buildObject(IExpressionContext context, String expressionObjectName) {
    return switch (expressionObjectName) {
      case COMP_UTIL -> compUtil;
      case EX_UTIL -> exUtil;
      case OPT_UTIL -> optUtil;
      case PROP_UTIL -> propUtil;
      case STR_UTIL -> strUtil;
      case THEME_COLOR_UTIL -> themeColorUtil;
      default -> null;
    };
  }

  @Override
  public boolean isCacheable(@Nullable String expressionObjectName) {
    // None of these hold per-request mutable state of their own (compUtil's HttpServletRequest
    // is itself a request-scoped proxy, safe to reuse across a single template execution).
    return true;
  }
}
