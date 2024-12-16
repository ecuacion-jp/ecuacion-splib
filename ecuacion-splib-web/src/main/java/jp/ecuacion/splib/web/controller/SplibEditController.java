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

import jakarta.annotation.Nonnull;
import jp.ecuacion.splib.web.bean.MessagesBean;
import jp.ecuacion.splib.web.bean.RedirectUrlBean;
import jp.ecuacion.splib.web.bean.RedirectUrlPageOnSuccessBean;
import jp.ecuacion.splib.web.bean.RedirectUrlPathBean;
import jp.ecuacion.splib.web.constant.SplibWebConstants;
import jp.ecuacion.splib.web.form.SplibEditForm;
import jp.ecuacion.splib.web.service.SplibEditService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

public abstract class SplibEditController<E extends SplibEditForm, S extends SplibEditService<E>>
    extends SplibGeneral1FormController<E, S> {

  private static final String PARAM_INSERT = "showInsertForm";
  private static final String PARAM_UPDATE = "showUpdateForm";

  private PageTemplatePatternEnum pageTemplatePattern;

  protected RedirectUrlPathBean redirectPathOnSuccess;

  public SplibEditController(PageTemplatePatternEnum pageTemplatePattern,
      @Nonnull String function) {
    this(pageTemplatePattern, function, new ControllerContext());
  }

  public SplibEditController(PageTemplatePatternEnum pageTemplatePattern, @Nonnull String function,
      ControllerContext settings) {
    super(function, settings.subFunction("edit"));

    this.pageTemplatePattern = pageTemplatePattern;
  }

  /** 処理成功時の表示画面のdefault。 */
  public String getDefaultSubFunctionOnSuccess() {
    return pageTemplatePattern == PageTemplatePatternEnum.SINGLE ? "edit" : "searchList";
  }

  /**
   * paramsをつけ忘れるとSplibGeneral1FormController#pageが呼ばれて気づかないうちに処理が変わると困るので、 明示的にわかるようシステムエラーとしておく。
   */
  @Override
  public String page(Model model, E form, @AuthenticationPrincipal UserDetails loginUser) {
    throw new RuntimeException("\"page\" needs params.");
  }

  @GetMapping(value = "page", params = PARAM_INSERT)
  public String showInsertPage(Model model, E form, @AuthenticationPrincipal UserDetails loginUser)
      throws Exception {
    return pageCommon(model, form, loginUser, true);
  }


  @GetMapping(value = "page", params = PARAM_UPDATE)
  public String showUpdatePage(Model model, E form, @AuthenticationPrincipal UserDetails loginUser)
      throws Exception {
    return pageCommon(model, form, loginUser, false);
  }

  private String pageCommon(Model model, E form, UserDetails loginUser, boolean isInsert)
      throws Exception {
    prepare(model, loginUser, new PrepareSettings().noRedirect(), form);

    // redirect元でエラーがあった場合はデータの再取得は行わない
    MessagesBean messageBean = (MessagesBean) model.getAttribute(MessagesBean.key);

    if (messageBean.getErrorMessages().size() == 0) {
      form.setIsInsert(isInsert);
      getService().page(form, loginUser);
    }

    getService().prepareForm(form, loginUser);

    return getFunction() + "Edit";
  }

  @PostMapping(value = "action", params = "insertOrUpdate")
  public String edit(@Validated E form, BindingResult result, Model model,
      @AuthenticationPrincipal UserDetails loginUser) throws Exception {
    prepare(model, loginUser,
        new PrepareSettings().addParam(form.isInsert() ? PARAM_INSERT : PARAM_UPDATE),
        form.validate(result));
    getService().edit(form, loginUser);

    RedirectUrlBean redirectBean =
        redirectPathOnSuccess == null ? new RedirectUrlPageOnSuccessBean() : redirectPathOnSuccess;
    redirectBean.putParam(SplibWebConstants.KEY_DATA_KIND, form.getDataKind());
    redirectBean.putParam(PARAM_UPDATE, form.getDataKind());

    return getReturnStringOnSuccess(redirectBean);
  }

  @PostMapping(value = "action", params = "back")
  public String back(@Validated E editForm, BindingResult result, Model model) {
    boolean isSingle = pageTemplatePattern == PageTemplatePatternEnum.SINGLE;
    
    String retunPageFunction = isSingle ? "edit" : "searchList";
    RedirectUrlBean rtnBean = new RedirectUrlPageOnSuccessBean(retunPageFunction, "page")
        .noSuccessMessage().putParam(SplibWebConstants.KEY_DATA_KIND, editForm.getDataKind());
    if (isSingle) {
      rtnBean.putParam("showUpdateForm", (String) null);
    }

    return getReturnStringOnSuccess(rtnBean);
  }

  public static enum PageTemplatePatternEnum {
    SINGLE, PAIR_WITH_SEARCH_LIST
  }
}
