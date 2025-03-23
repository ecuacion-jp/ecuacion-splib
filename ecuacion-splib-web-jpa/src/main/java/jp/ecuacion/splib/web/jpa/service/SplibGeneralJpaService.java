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
package jp.ecuacion.splib.web.jpa.service;

import jp.ecuacion.lib.jpa.entity.EclibEntity;
import jp.ecuacion.splib.web.service.SplibGeneralService;
import org.springframework.transaction.annotation.Transactional;

/**
 * Is the service abstract class for general page of the page template with JPA.
 * 
 * @param <E> entity
 */
@Transactional(rollbackFor = Exception.class)
public abstract class SplibGeneralJpaService<E extends EclibEntity> extends SplibGeneralService
    implements SplibJpaServiceInterface<E> {

}
