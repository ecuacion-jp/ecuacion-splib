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
package jp.ecuacion.splib.jpa.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

/**
 * Provides epository methods needed for soft delete.
 * 
 * @param <T> The entity class
 * @param <I> The data type of the surrogate key, like {@code Long}.
 */
@NoRepositoryBean
public interface SplibRepository<T, I> extends JpaRepository<T, I> {
  
  /**
   * Is used to physical-delete soft-deleted records.
   * 
   * @param entity entity
   * @return Optional of the entity.
   */
  Optional<T> findByIdAndSoftDeleteFieldTrueFromAllGroups(@Param("entity") T entity);

  /**
   * Is used to physical-delete soft-deleted records.
   * 
   * @param entity entity
   * @return Optional of the entity.
   */
  Optional<T> findByNaturalKeyAndSoftDeleteFieldTrueFromAllGroups(@Param("entity") T entity);

  /**
   * Is used to physical-delete soft-deleted records.
   * 
   * @param entity entity
   */
  @Modifying
  void deleteByIdAndSoftDeleteFieldTrueFromAllGroups(@Param("entity") T entity);

}
