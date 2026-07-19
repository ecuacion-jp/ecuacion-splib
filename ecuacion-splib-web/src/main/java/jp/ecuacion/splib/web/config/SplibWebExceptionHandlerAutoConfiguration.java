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
package jp.ecuacion.splib.web.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.util.ExceptionUtil;
import jp.ecuacion.lib.core.util.LogUtil;
import jp.ecuacion.lib.core.util.PropertiesFileUtil;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.lib.core.violation.Violations.MessageParameters;
import jp.ecuacion.splib.web.constant.SplibWebConstants;
import jp.ecuacion.splib.web.exceptionhandler.SplibExceptionHandler;
import jp.ecuacion.splib.web.util.SplibValidationHelper;
import jp.ecuacion.splib.web.util.internal.RefererRedirectUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Auto-configures a minimal {@link ViolationException} handler when no
 * {@link SplibExceptionHandler} bean is present in the application context.
 *
 * <p>This allows applications that use only Jakarta Validation via
 *     {@link jp.ecuacion.splib.web.util.SplibValidationHelper} to display error messages
 *     without defining a full exception handler. Only {@link ViolationException} is handled;
 *     all other exceptions fall through to Spring Boot's default error handling.</p>
 *
 * <p>When {@link SplibValidationHelper#validate(Object, String)} is called with a return view
 *     name, the handler forwards to that view so that {@code th:errors} field-level display
 *     works. When called without a view name, it redirects to the referring page instead.</p>
 *
 * <p>Message display is controlled by {@code application.properties}:</p>
 * <ul>
 *   <li>{@code jp.ecuacion.splib.web.process-result-message.shown-at-the-top} (default:
 *       {@code true}) — shows messages with item names at the top of the page.</li>
 *   <li>{@code jp.ecuacion.splib.web.process-result-message.shown-at-each-item} (default:
 *       {@code false}) — shows messages without item names next to each field.</li>
 * </ul>
 */
@AutoConfiguration
public class SplibWebExceptionHandlerAutoConfiguration {

  private static final String PROP_AT_TOP =
      "jp.ecuacion.splib.web.process-result-message.shown-at-the-top";
  private static final String PROP_AT_ITEM =
      "jp.ecuacion.splib.web.process-result-message.shown-at-each-item";

  /**
   * Minimal {@link ControllerAdvice} that handles only {@link ViolationException}.
   */
  @ControllerAdvice
  static class SplibValidationExceptionHandler {

    private final DetailLogger detailLog = new DetailLogger(this);

    private final HttpServletRequest request;

    /**
     * Constructs a new instance.
     *
     * @param request request
     */
    SplibValidationExceptionHandler(HttpServletRequest request) {
      this.request = request;
    }

    /**
     * Handles {@link ViolationException}.
     *
     * <p>If a return view name was set via
     *     {@link SplibValidationHelper#validate(Object, String)}, forwards to that view.
     *     Otherwise redirects to the referring page.</p>
     *
     * @param exception the violation exception
     * @param model model
     * @param redirectAttributes redirect attributes
     * @return ModelAndView forwarding or redirecting on error
     */
    @SuppressWarnings("null")
    @ExceptionHandler(ViolationException.class)
    public ModelAndView handleViolationException(ViolationException exception, Model model,
        RedirectAttributes redirectAttributes) {

      Violations violations = exception.getViolations();
      MessageParameters params = violations.messageParameters();
      Locale locale = request.getLocale();

      List<ConstraintViolation<?>> sortedCvs = violations.getConstraintViolations().stream()
          .sorted(Comparator.comparing(cv -> cv.getPropertyPath().toString())).toList();

      String viewName =
          (String) request.getAttribute(SplibValidationHelper.REQUEST_ATTR_RETURN_VIEW);

      if (viewName != null) {
        return handleAsForward(sortedCvs, violations.getBusinessViolations(), params, locale,
            viewName, model);
      }

      return handleAsRedirect(sortedCvs, violations.getBusinessViolations(), params, locale,
          redirectAttributes);
    }

    @SuppressWarnings("null")
    private ModelAndView handleAsForward(List<ConstraintViolation<?>> sortedCvs,
        List<BusinessViolation> businessViolations, MessageParameters params, Locale locale,
        String viewName, Model model) {

      boolean atTop =
          Boolean.parseBoolean(PropertiesFileUtil.getApplicationOrElse(PROP_AT_TOP, "false"));
      boolean atItem =
          Boolean.parseBoolean(PropertiesFileUtil.getApplicationOrElse(PROP_AT_ITEM, "true"));

      if (atTop) {
        List<String> topMessages = new ArrayList<>();
        for (ConstraintViolation<?> cv : sortedCvs) {
          topMessages.addAll(ExceptionUtil
              .getMessageList(new Violations().messageParameters(params).add(cv), locale, true));
        }
        for (BusinessViolation bv : businessViolations) {
          topMessages.addAll(ExceptionUtil.getMessageList(new Violations().add(bv), locale, true));
        }
        model.addAttribute(SplibWebConstants.KEY_GLOBAL_ERRORS, topMessages);
      }

      if (atItem) {
        for (ConstraintViolation<?> cv : sortedCvs) {
          Object rootBean = cv.getRootBean();
          String formName = StringUtils.uncapitalize(rootBean.getClass().getSimpleName());
          String brKey = BindingResult.MODEL_KEY_PREFIX + formName;
          BeanPropertyBindingResult br =
              model.getAttribute(brKey) instanceof BeanPropertyBindingResult b ? b
                  : new BeanPropertyBindingResult(rootBean, formName);
          String propertyPath = cv.getPropertyPath().toString();
          String errorCode =
              cv.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName();
          String message = ExceptionUtil
              .getMessageList(new Violations().messageParameters(params).add(cv), locale, false)
              .get(0);
          br.rejectValue(propertyPath, errorCode, message);
          model.addAttribute(brKey, br);
        }
      }

      return new ModelAndView(viewName, model.asMap());
    }

    private ModelAndView handleAsRedirect(List<ConstraintViolation<?>> sortedCvs,
        List<BusinessViolation> businessViolations, MessageParameters params, Locale locale,
        RedirectAttributes redirectAttributes) {

      List<String> errorMessages = new ArrayList<>();
      for (ConstraintViolation<?> cv : sortedCvs) {
        errorMessages.addAll(ExceptionUtil
            .getMessageList(new Violations().messageParameters(params).add(cv), locale, false));
      }
      for (BusinessViolation bv : businessViolations) {
        errorMessages.addAll(ExceptionUtil.getMessageList(new Violations().add(bv), locale, false));
      }
      redirectAttributes.addFlashAttribute(SplibWebConstants.KEY_GLOBAL_ERRORS, errorMessages);

      // The Referer header is client-controlled; see RefererRedirectUtil's javadoc for why a
      // same-origin check is needed even after taking only the path and query.
      String redirectTarget = "/";
      String referer = request.getHeader("Referer");
      if (referer != null) {
        try {
          redirectTarget = RefererRedirectUtil.toSameOriginRedirectTarget(referer);
        } catch (IllegalArgumentException ex) {
          LogUtil.logSystemError(detailLog, ex);
        }
      }
      return new ModelAndView("redirect:" + redirectTarget);
    }
  }

  /**
   * Provides {@link SplibValidationExceptionHandler} when no {@link SplibExceptionHandler}
   * bean is present.
   *
   * @param request request
   * @return a minimal validation exception handler
   */
  @Bean
  @ConditionalOnMissingBean(SplibExceptionHandler.class)
  SplibValidationExceptionHandler splibValidationExceptionHandler(HttpServletRequest request) {
    return new SplibValidationExceptionHandler(request);
  }
}
