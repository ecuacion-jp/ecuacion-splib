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
package jp.ecuacion.splib.web.controller;

import java.util.Objects;
import jp.ecuacion.splib.web.bean.ReturnUrlBuilder;
import jp.ecuacion.splib.web.constant.SplibWebConstants;
import jp.ecuacion.splib.web.exception.RedirectToHomePageException;
import jp.ecuacion.splib.web.form.SplibEditForm;
import jp.ecuacion.splib.web.service.SplibEditService;
import jp.ecuacion.splib.web.util.SplibLoginStateUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Controls the edit feature.
 * 
 * @param <F> SplibEditForm
 * @param <S> SplibEditService
 */
public abstract class SplibEditController<F extends SplibEditForm, S extends SplibEditService<F>>
    extends SplibGeneral1FormController<F, S> {

  private static final String PARAM_INSERT = "showInsertForm";
  private static final String PARAM_UPDATE = "showUpdateForm";

  /**
   * See {@link PageTemplatePatternEnum}.
   */
  private PageTemplatePatternEnum pageTemplatePattern;

  @Nullable
  protected ReturnUrlBuilder redirectOnSuccess;

  @Autowired
  private SplibLoginStateUtil loginStateUtil;

  /**
   * Construct a new instance with {@code function}.
   * 
   * @param function function
   */
  public SplibEditController(PageTemplatePatternEnum pageTemplatePattern,
      String function) {
    this(pageTemplatePattern, function, new ControllerContext());
  }

  /**
   * Construct a new instance with {@code function}, {@code settings}.
   * 
   * @param function function
   * @param settings settings
   */
  public SplibEditController(PageTemplatePatternEnum pageTemplatePattern, String function,
      ControllerContext settings) {
    super(function, settings.subFunction("edit"));

    this.pageTemplatePattern = pageTemplatePattern;
  }

  /**
   * Specifies return value which depends on the value of {@code PageTemplatePatternEnum}.
   */
  @Override
  public String getDefaultDestSubFunctionOnNormalEnd() {
    return pageTemplatePattern == PageTemplatePatternEnum.SINGLE ? "edit" : "searchList";
  }

  /**
   * Throws {@code RuntimeException} to avoid the mistake.
   * 
   * <p>In {@code SplibEditController} {@code page} is not used, 
   *     but {@code showInsertPage} and {@code showUpdatePage} is used.
   *     If {@code page} is called, un-assumed procedure runs and difficult to debug,
   *     so make things easier by throwing exception.</p>
   */
  @Override
  public String page(Model model, F form, @AuthenticationPrincipal UserDetails loginUser) {
    throw new RedirectToHomePageException("jp.ecuacion.splib.web.common.message.urlNotProper");
  }

  /**
   * Shows insert page.
   * 
   * @param model model
   * @param form form
   * @param loginUser loginUser
   * @return URL
   * @throws Exception Exception
   */
  @GetMapping(value = "page", params = "action=" + PARAM_INSERT)
  public String showInsertPage(Model model, F form, @AuthenticationPrincipal UserDetails loginUser)
      throws Exception {
    return pageCommon(model, form, loginUser, true);
  }

  /**
   * Shows update page.
   * 
   * @param model model
   * @param form form
   * @param loginUser loginUser
   * @return URL
   * @throws Exception Exception
   */
  @GetMapping(value = "page", params = "action=" + PARAM_UPDATE)
  public String showUpdatePage(Model model, F form, @AuthenticationPrincipal UserDetails loginUser)
      throws Exception {
    return pageCommon(model, form, loginUser, false);
  }

  private String pageCommon(Model model, F form, UserDetails loginUser, boolean isInsert)
      throws Exception {
    prepare(model, loginUser, form);

    // Do not re-fetch data if there was an error at the redirect source.
    BindingResult bindingResult = (BindingResult) model.getAttribute(
        BindingResult.MODEL_KEY_PREFIX + StringUtils.uncapitalize(form.getClass().getSimpleName()));

    if (bindingResult == null || !bindingResult.hasErrors()) {
      form.setIsInsert(isInsert);
      getService().page(form, loginUser);
    }

    getService().prepareForm(form, loginUser);

    return getDefaultHtmlPageName();
  }

  /**
   * Edits (= inserts or updates) specified record.
   *
   * @param form form
   * @param model model
   * @param loginUser loginUser
   * @return URL
   * @throws Exception Exception
   */
  @PostMapping(value = "action", params = "action=insertOrUpdate")
  public String edit(F form, Model model, @AuthenticationPrincipal UserDetails loginUser)
      throws Exception {
    addParamToParamListOnRedirect("action",
        Boolean.TRUE.equals(form.isInsert()) ? PARAM_INSERT : PARAM_UPDATE);
    prepare(model, loginUser, form.validate(null));
    getService().edit(form, loginUser);

    @NonNull ReturnUrlBuilder redirectBuilder =
        redirectOnSuccess == null
            ? ReturnUrlBuilder.forNormalEnd(this, loginStateUtil).showSuccessMessage()
            : Objects.requireNonNull(redirectOnSuccess);
    redirectBuilder.putParam(SplibWebConstants.KEY_DATA_KIND, form.getDataKind());
    redirectBuilder.putParam("action", PARAM_UPDATE);

    return redirectBuilder.getUrl();
  }

  /**
   * Returns the prior page.
   *
   * @param editForm editForm
   * @param model Model
   * @return URL
   */
  @PostMapping(value = "action", params = "action=back")
  public String back(F editForm, Model model) {
    boolean isSingle = pageTemplatePattern == PageTemplatePatternEnum.SINGLE;

    String retunPageFunction = isSingle ? "edit" : "searchList";
    ReturnUrlBuilder rtnBuilder = ReturnUrlBuilder.forNormalEnd(this, loginStateUtil)
        .toSubFunction(retunPageFunction).toPage("page")
        .putParam(SplibWebConstants.KEY_DATA_KIND, editForm.getDataKind());
    if (isSingle) {
      rtnBuilder.putParam("showUpdateForm", (String) null);
    }

    return rtnBuilder.getUrl();
  }

  /**
   * Specifies the pair controller that consists of the function with this controller.
   * 
   * <p>This reflects the return value of {@code getDefaultDestSubFunctionOnNormalEnd}.</p>
   * 
   * <ul>
   * <li>{@code SINGLE} means no pair exists.</li>
   * <li>{@code PAIR_WITH_SEARCH_LIST} means 
   *     the controller pairs with {@link SplibSearchListController}.</li>
   * </ul>
   */
  public static enum PageTemplatePatternEnum {
    SINGLE, PAIR_WITH_SEARCH_LIST
  }
}
