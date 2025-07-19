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
import java.util.ArrayList;
import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.splib.web.bean.MessagesBean;
import jp.ecuacion.splib.web.bean.ReturnUrlBean;
import jp.ecuacion.splib.web.constant.SplibWebConstants;
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

/**
 * Controls the search and listing of the search result.
 * 
 * @param <FST> SplibSearchForm
 * @param <FLT> SplibListForm
 * @param <S> SplibSearchListService
 */
//@formatter:off
public abstract class SplibSearchListController<FST extends SplibSearchForm, 
    FLT extends SplibListForm<?>, S extends SplibSearchListService<FST, FLT>> 
    extends SplibGeneral2FormsController<FST, FLT, S> {
  //@formatter:on


  /**
   * Is used when the search condition is cleared.
   * 
   * <p>It's used only when the search condition clears, it's better to add {@code ＠Lazy},
   * but it occurs an error at the cast procedure in
   * {@code jp.ecuacion.splib.web.jpa.service.SplibSearchListJpaService#getSpecs}.
   * Maybe a bug of spring mvc.
   * </p>
   */
  @Autowired
  private FST newSearchForm;

  @Autowired
  private SplibUtil util;

  /**
   * Construct a new instance with {@code function}.
   * 
   * @param function function
   */
  public SplibSearchListController(@Nonnull String function) {
    this(function, new ControllerContext());
  }

  /**
   * Construct a new instance with {@code function}, {@code settings}.
   * 
   * @param function function
   * @param settings settings
   */
  public SplibSearchListController(@Nonnull String function, ControllerContext settings) {
    super(function, settings.subFunction("searchList"));
  }

  @Override
  public String getDefaultDestSubFunctionOnNormalEnd() {
    return "searchList";
  }

  /**
   * Overrides the parent method to add {@code getProperSearchForm} procedure.
   */
  @Override
  public String submitOnChangeToRefresh(Model model, FST searchForm, FLT listForm,
      @AuthenticationPrincipal UserDetails loginUser) throws Exception {
    // 最新searchFormをsessionに保存
    searchForm = getProperSearchForm(model, searchForm);
    super.submitOnChangeToRefresh(model, searchForm, listForm, loginUser);

    return new ReturnUrlBean(this, util, "searchList", "page").getUrl();
  }

  /**
   * Overrides the parent method to add {@code getProperSearchForm, listForm.setDataKind() 
   * and redirectUrlOnAppExceptionBean} procedures.
   */
  @Override
  public String page(Model model, FST searchForm, FLT listForm,
      @AuthenticationPrincipal UserDetails loginUser) throws Exception {
    searchForm = getProperSearchForm(model, searchForm);
    listForm.setDataKind(searchForm.getDataKind());
    redirectUrlOnAppExceptionBean = new ReturnUrlBean(this, util, false);

    prepare(model, loginUser, searchForm, listForm);

    getService().prepareForm(searchForm, listForm, loginUser);

    // Do not search when search condition has errors
    MessagesBean mb = (MessagesBean) model.getAttribute(SplibWebConstants.KEY_MESSAGES_BEAN);
    if (mb.getErrorMessages().size() == 0) {
      getService().page(searchForm, listForm, loginUser);

    } else {
      // Does something needed to search result of zero list.
      listForm.setRecList(new ArrayList<>());
      searchForm.setNumberOfRecordsAndAdjustCurrentPageNumger(0L);
    }

    return getDefaultHtmlPageName();
  }

  /**
   * Searches from the search conditions in {@code searchForm}.
   * 
   * <p>This method only redirects to {@code page} 
   *     because the actual search procedure is implemented at {@code page} method.</p>
   * 
   * @param model model
   * @param searchForm searchForm
   * @param listForm listForm
   * @param loginUser loginUser
   * @return URL
   * @throws Exception Exception
   */
  @GetMapping(value = "action", params = "search")
  public String search(Model model, FST searchForm, FLT listForm,
      @AuthenticationPrincipal UserDetails loginUser) throws Exception {

    // Prepare searchForm before validating it.
    // This is meaningful when showing errors on opening searchList page
    // by not calling ".../searchList/page" but ".../searchList/action?search"
    // to validate searchForm.
    prepareForm(searchForm, listForm, loginUser);
    
    prepare(model, searchForm.validate(null), listForm);
    return redirectToSamePageTakingOverModel(model);
  }

  /**
   * Searches from the search conditions in {@code searchForm}.
   * 
   * <p>This is exactly the same procedure as {@code search}, but there seems to be no way
   *     to integrate these
   *     because multiple {@code @GetMapping} cannot be added to a single method.</p>
   *     
   * @param model model
   * @param searchForm searchForm
   * @param listForm listForm
   * @param loginUser loginUser
   * @return URL
   * @throws Exception Exception
   */
  @GetMapping(value = "action", params = "action=searchAgain")
  public String searchAgain(Model model, FST searchForm, FLT listForm,
      @AuthenticationPrincipal UserDetails loginUser) throws Exception {
    return search(model, searchForm, listForm, loginUser);
  }

  private void prepareForm(FST searchForm, FLT listForm, UserDetails loginUser) {
    if (!searchForm.isPrepared()) {
      getService().prepareForm(searchForm, listForm, loginUser);
      searchForm.setPrepared(true);
    }
  }

  /**
   * Clears search conditions.
   * 
   * @param model model
   * @param searchForm searchForm
   * @param listForm listForm
   * @param loginUser loginUser
   * @return URL
   * @throws Exception Exception
   */
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
    return new ReturnUrlBean(this, util, true)
        .putParam(SplibWebConstants.KEY_DATA_KIND, searchForm.getDataKind()).getUrl();
  }

  /**
   * Returns proper search form.
   * 
   * <p>This feature stores the search conditions in {@code session}, 
   *     so the search conditions are recorded 
   *     even if you go to the other function pages and come back.</p>
   * 
   * @param model model
   * @param searchForm searchForm
   * @return proper searchForm
   */
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
      // その前に検索しているはずなので、引数のsearchFormがnullで、かつsessionにも情報がない、という場合はあり得ない。
      if (request.getSession().getAttribute(formName) == null) {
        throw new RuntimeException("searchForm == null cannot be occurred.");
      }
    }

    @SuppressWarnings("unchecked")
    FST formUsedForSearch =
        (FST) request.getSession().getAttribute(getSessionKey(formName, searchForm));

    return formUsedForSearch;
  }

  /* fがnullの場合null Pointerが発生するためメソッド冒頭で定義することはできず、別メソッドとした。 */
  private String getSessionKey(String formName, FST searchForm) {
    return formName + (searchForm == null || searchForm.getDataKind() == null
        || searchForm.getDataKind().equals("") ? "" : "." + searchForm.getDataKind());
  }

  /**
   * Deletes the specified record.
   * 
   * @param model model
   * @param searchForm searchForm
   * @param listForm listForm
   * @param loginUser loginUser
   * @return URL
   * @throws Exception Exception
   */
  @PostMapping(value = "action", params = "delete")
  public String delete(Model model, FST searchForm, FLT listForm,
      @AuthenticationPrincipal UserDetails loginUser) throws Exception {
    prepare(model, loginUser, searchForm, listForm);
    getService().delete(listForm, loginUser);

    return new ReturnUrlBean(this, util, true).showSuccessMessage()
        .putParam(SplibWebConstants.KEY_DATA_KIND, listForm.getDataKind()).getUrl();
  }

  /**
   * Shows edit page in insert mode.
   * 
   * @param model model
   * @param loginUser loginUser
   * @return URL
   * @throws AppException AppException
   */
  @PostMapping(value = "action", params = "showInsertForm")
  public String showInsertForm(Model model, FST searchForm, FLT listForm,
      @AuthenticationPrincipal UserDetails loginUser) throws AppException {
    prepare(model, loginUser, searchForm, listForm);
    ReturnUrlBean bean =
        new ReturnUrlBean(this, util, "edit", "page").putParamMap(request.getParameterMap());
    return bean.getUrl();
  }

  /**
   * Shows edit page in update mode.
   * 
   * @param model model
   * @param loginUser loginUser
   * @return URL
   * @throws AppException AppException
   */
  @PostMapping(value = "action", params = "showUpdateForm")
  public String showUpdateForm(Model model, FST searchForm, FLT listForm,
      @AuthenticationPrincipal UserDetails loginUser) throws AppException {
    prepare(model, loginUser, searchForm, listForm);
    ReturnUrlBean bean =
        new ReturnUrlBean(this, util, "edit", "page").putParamMap(request.getParameterMap());
    return bean.getUrl();
  }
}
