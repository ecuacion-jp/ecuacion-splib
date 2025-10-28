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
package jp.ecuacion.splib.web.jpa.controller;

import jakarta.annotation.Nonnull;
import jp.ecuacion.splib.web.controller.SplibSearchListController;
import jp.ecuacion.splib.web.form.SplibListForm;
import jp.ecuacion.splib.web.form.SplibSearchForm;
import jp.ecuacion.splib.web.jpa.service.SplibSearchListJpaService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;

/**
 * Controls the search and listing of the search result.
 * 
 * @param <FST> SplibSearchForm
 * @param <FLT> SplibListForm
 * @param <S> SplibSearchListService
 */
//@formatter:off
public abstract class SplibSearchListJpaController
    <FST extends SplibSearchForm, FLT extends SplibListForm<?>, 
    S extends SplibSearchListJpaService<FST, FLT, ?>> 
    extends SplibSearchListController<FST, FLT, S> {
  //@formatter:on

  /**
   * Construct a new instance with {@code function}.
   * 
   * @param function function
   */
  public SplibSearchListJpaController(@Nonnull String function) {
    super(function);
  }

  /**
   * Construct a new instance with {@code function}, {@code settings}.
   * 
   * @param function function
   * @param settings settings
   */
  public SplibSearchListJpaController(@Nonnull String function, ControllerContext settings) {
    super(function, settings);
  }

  @Override
  public String page(Model model, FST searchForm, FLT listForm,
      @AuthenticationPrincipal UserDetails loginUser) throws Exception {

    return super.page(model, searchForm, listForm, loginUser);
  }
}
