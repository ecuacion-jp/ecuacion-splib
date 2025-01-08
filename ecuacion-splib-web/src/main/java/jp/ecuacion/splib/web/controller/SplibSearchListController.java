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
import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.splib.web.bean.RedirectUrlBean;
import jp.ecuacion.splib.web.bean.RedirectUrlPageOnAppExceptionBean;
import jp.ecuacion.splib.web.bean.RedirectUrlPageOnSuccessBean;
import jp.ecuacion.splib.web.constant.SplibWebConstants;
import jp.ecuacion.splib.web.exception.FormInputValidationException;
import jp.ecuacion.splib.web.form.SplibListForm;
import jp.ecuacion.splib.web.form.SplibSearchForm;
import jp.ecuacion.splib.web.service.SplibSearchListService;
import jp.ecuacion.splib.web.util.SplibUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

//@formatter:off
public abstract class SplibSearchListController<FST extends SplibSearchForm, 
    FLT extends SplibListForm<?>, S extends SplibSearchListService<FST, FLT>> 
    extends SplibGeneral2FormsController<FST, FLT, S> {
  //@formatter:on


  /**
   * 検索条件クリアの際に使用。 検索条件の削除の場合にしか使用しないので、＠Lazyをつけておくとより効率的なのだが、
   * ＠LazyをつけるとSのAutowired自体は成功するものの、元のxxxFormでcastしようとするとエラーになってしまった
   * （XxxSearchListService#getSpecsにて発生）のでLazyは削除。
   */
  @Autowired
  private FST newSearchForm;
  
  @Autowired
  private SplibUtil util;

  public SplibSearchListController(@Nonnull String function) {
    this(function, new ControllerContext());
  }

  public SplibSearchListController(@Nonnull String function, ControllerContext settings) {
    super(function, settings.subFunction("searchList"));
  }

  @Override
  public String getDefaultRedirectDestSubFunctionOnSuccess() {
    return "searchList";
  }

  /** 基本的にはSearch側の項目で使用すると想定されるため、overrideの引数としてはSearchForm側を渡しておく。 */
  @Override
  public String submitOnChangeToRefresh(Model model, FST searchForm, FLT listForm,
      @AuthenticationPrincipal UserDetails loginUser) throws Exception {
    // 最新searchFormをsessionに保存
    getProperSearchForm(model, searchForm);
    super.submitOnChangeToRefresh(model, searchForm, listForm, loginUser);

    return getReturnStringOnSuccess(new RedirectUrlPageOnSuccessBean(this, util, "searchList", "page"));
  }

  /** listFormはparameterを受け取るわけではないが、instance生成のため引数に付加。 */
  @Override
  public String page(Model model, FST searchForm, FLT listForm,
      @AuthenticationPrincipal UserDetails loginUser) throws Exception {
    searchForm = getProperSearchForm(model, searchForm);
    listForm.setDataKind(searchForm.getDataKind());
    redirectUrlOnAppExceptionBean = new RedirectUrlPageOnAppExceptionBean(this, util);

    prepare(model, loginUser, searchForm, listForm);
    getService().page(searchForm, listForm, loginUser);
    getService().prepareForm(searchForm, listForm, loginUser);

    return getReturnStringToShowPage();
  }

  /** 本来はpage()で直接受けたいが、1 methodに複数のGetMappingを設定できないため別メソッドとした。 */
  @GetMapping(value = "action", params = "search")
  public String search(Model model, FST searchForm, FLT listForm,
      @AuthenticationPrincipal UserDetails loginUser) throws Exception {

    prepare(model, searchForm, listForm);
    return prepareForRedirectOrForwardAndGetPath(
        new RedirectUrlPageOnSuccessBean(this, util).noSuccessMessage(), model);
  }

  /** 本来はpage()で直接受けたいが、1 methodに複数のGetMappingを設定できないため別メソッドとした。 */
  @GetMapping(value = "action", params = "action=searchAgain")
  public String searchAgain(Model model, FST searchForm,
      @AuthenticationPrincipal UserDetails loginUser, FLT listForm) throws Exception {

    prepare(model, searchForm, listForm);
    return prepareForRedirectOrForwardAndGetPath(
        new RedirectUrlPageOnSuccessBean(this, util).noSuccessMessage(), model);
  }

  @SuppressWarnings({"unchecked"})
  protected FST getProperSearchForm(Model model, FST searchForm) {

    String formName = getFunction() + "SearchForm";
    String key = getSessionKey(formName, searchForm);
    if (searchForm != null) {
      // 検索画面上で検索された場合はその条件を採用し保管。
      // それ以外の場合（メニューのリンクなどから来た場合など）はmodelに設定されたものをそのまま使用。（ここでは何もしない）
      if (searchForm.isRequestFromSearchForm()) {
        request.getSession().setAttribute(key, searchForm);
      }

      // 初回アクセスでsessionに存在しない場合は引数のformを使用
      if (request.getSession().getAttribute(key) == null) {
        request.getSession().setAttribute(key, searchForm);
      }

    } else {
      // その前に検索しているはずなので、引数のfがnullで、かつsessionにも情報がない、という場合はあり得ない。
      if (request.getSession().getAttribute(formName) == null) {
        throw new RuntimeException("f == null cannot be occurred.");
      }
    }

    FST formUsedForSearch =
        (FST) request.getSession().getAttribute(getSessionKey(formName, searchForm));

    return formUsedForSearch;
  }

  /** fがnullの場合null Pointerが発生するためメソッド冒頭で定義することはできず、別メソッドとした。 */
  private String getSessionKey(String formName, FST searchForm) {
    return formName + (searchForm == null || searchForm.getDataKind() == null
        || searchForm.getDataKind().equals("") ? "" : "." + searchForm.getDataKind());
  }

  @GetMapping(value = "action", params = "conditionClear")
  public String searchConditionClear(Model model, FST searchForm, FLT listForm,
      @AuthenticationPrincipal UserDetails loginUser) throws Exception {
    // 情報をクリアするための設定を行う
    String formName = getFunction() + "SearchForm";
    String sessionKey =
        formName + (searchForm.getDataKind() == null || searchForm.getDataKind().equals("") ? ""
            : "." + searchForm.getDataKind());

    request.getSession().setAttribute(sessionKey, newSearchForm);

    prepare(model, loginUser, searchForm, listForm);
    return getReturnStringOnSuccess(new RedirectUrlPageOnSuccessBean(this, util).noSuccessMessage()
        .putParam(SplibWebConstants.KEY_DATA_KIND, searchForm.getDataKind()));
  }

  @PostMapping(value = "action", params = "delete")
  public String delete(Model model, FST searchForm, FLT listForm,
      @AuthenticationPrincipal UserDetails loginUser) throws Exception {
    prepare(model, loginUser, searchForm, listForm);
    getService().delete(listForm, loginUser);

    return getReturnStringOnSuccess(new RedirectUrlPageOnSuccessBean(this, util)
        .putParam(SplibWebConstants.KEY_DATA_KIND, listForm.getDataKind()));
  }

  @PostMapping(value = "action", params = "showInsertForm")
  public String showInsertForm(Model model, @AuthenticationPrincipal UserDetails loginUser)
      throws FormInputValidationException, AppException {
    prepare(model, loginUser);
    RedirectUrlBean bean = new RedirectUrlPageOnSuccessBean(this, util, "edit", "page").noSuccessMessage()
        .putParamMap(request.getParameterMap());
    return getReturnStringOnSuccess(bean);
  }

  @PostMapping(value = "action", params = "showUpdateForm")
  public String showUpdateForm(Model model, @AuthenticationPrincipal UserDetails loginUser)
      throws FormInputValidationException, AppException {
    prepare(model, loginUser);
    RedirectUrlBean bean = new RedirectUrlPageOnSuccessBean(this, util, "edit", "page").noSuccessMessage()
        .putParamMap(request.getParameterMap());
    return getReturnStringOnSuccess(bean);
  }
}
