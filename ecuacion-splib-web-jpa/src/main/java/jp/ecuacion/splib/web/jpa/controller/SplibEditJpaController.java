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
import jp.ecuacion.splib.web.controller.SplibEditController;
import jp.ecuacion.splib.web.form.SplibEditForm;
import jp.ecuacion.splib.web.jpa.service.SplibEditJpaService;

/**
 * Controls the edit feature.
 * 
 * @param <F> SplibEditForm
 * @param <S> SplibEditService
 */
//@formatter:off
public abstract class SplibEditJpaController 
    <F extends SplibEditForm, S extends SplibEditJpaService<F, ?>>
    extends SplibEditController<F, S> {
  //@formatter:on

  /**
   * Construct a new instance with {@code function}.
   * 
   * @param function function
   */
  public SplibEditJpaController(PageTemplatePatternEnum pageTemplatePattern,
      @Nonnull String function) {
    super(pageTemplatePattern, function);
  }

  /**
   * Construct a new instance with {@code function}, {@code settings}.
   * 
   * @param function function
   * @param settings settings
   */
  public SplibEditJpaController(PageTemplatePatternEnum pageTemplatePattern,
      @Nonnull String function, ControllerContext settings) {
    super(pageTemplatePattern, function, settings);
  }
}
