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
package jp.ecuacion.splib.jpa.bl;

import java.util.Optional;
import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.lib.core.exception.checked.BizLogicAppException;
import jp.ecuacion.lib.jpa.entity.EclibEntity;
import jp.ecuacion.splib.core.bl.SplibCoreBl;
import jp.ecuacion.splib.jpa.repository.SplibRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * Offers JPA related business logics.
 * 
 * @param <E> Entity
 * @param <I> Id of entity
 * @param <V> Version of entity
 */
public abstract class SplibJpaBl<E extends EclibEntity, I, V> extends SplibCoreBl {

  /**
   * Is used for {@code findAndOptimisticLockingCheck()}.
   * 
   * <p>It obtains the repository for select record from database.
   *     See {@link #findAndOptimisticLockingCheck(I, V)}.</p>
   * 
   * @return repository
   */
  public abstract SplibRepository<E, I> getRepositoryForOptimisticLocking();

  /**
   * Is used for {@code findAndOptimisticLockingCheck()}.
   * 
   * <p>It obtains the versions for optimistic exclusive control.
   *     See {@link #findAndOptimisticLockingCheck(String, String...)}.</p>
   */
  public abstract V getVersionForOptimisticLocking(E e);

  /**
   * Performs an optimistic exclusive control check when transitioning from a list screen
   * to a detail screen, or during editing or deletion, and also retrieves the entity from the DB.
   * This method compares the version retrieved when the screen was displayed and embedded in
   * the html with the version of the entity retrieved within this process.
   * Although the current difference is verified here, the optimistic check is performed once
   * more before the goal is achieved, so always use the entity returned by this method in
   * subsequent processing.
   * (For example, when updating data, after confirming no version difference in this check,
   * it is also necessary to treat it as an error if another user updates the record before the
   * update process is executed.)
   *
   * <p>For a single table, simply store one version value.
   * When updating multiple tables at once, the version of each table must be compared,
   * so versions are held in an array format.
   * (Although multiple elements could be stored in each string using a comma separator,
   * an array is used to avoid concerns about delimiter characters.
   * If a two-dimensional array would be needed, store multiple values in one element
   * using a comma separator or similar.)
   * </p>
   *
   * <p>In that case the screen holds multiple values stored in a String array,
   * and the version retrieved from the DB is also shaped the same way for comparison.
   * Regardless of which tables are being updated, if even one version differs it is an error.
   * Since this is not a high-frequency occurrence, the policy is: if any table has been
   * updated, re-confirm before proceeding with row updates.
   * </p>
   *
   * <p>When updating tables without relations simultaneously,
   * getVersionsForOptimisticLocking(E e) alone is insufficient,
   * so additional values must be retrieved at the called destination.
   * Note that the additionally retrieved entities must also be used in subsequent processing.
   * </p>
   */
  public E findAndOptimisticLockingCheck(I id, V version) throws AppException {
    Optional<E> optional = getRepositoryForOptimisticLocking().findById(id);

    // Another user may have deleted the record, setting the soft-delete flag
    // so it cannot be found. To account for this, treat a missing record by ID
    // as an exclusive control error.
    if (optional.isEmpty()) {
      String msg = "jp.ecuacion.splib.web.common.message.sameRecordAlreadyDeleted";
      throw new BizLogicAppException(msg);
    }

    E e = optional.get();

    // If versions differ, throw an optimistic exclusive control error.
    if (!isVersionsTheSame(version, getVersionForOptimisticLocking(e))) {
      throw new ObjectOptimisticLockingFailureException("some class", id);
    }

    return e;
  }

  private boolean isVersionsTheSame(V vers1, V vers2) {
    // null is not allowed. If the value is gone (e.g. after deletion), use a zero-length array.
    if (vers1 == null || vers2 == null) {
      throw new RuntimeException(
          "Optimistic Locking check cannot be done because version array is null.");
    }

    return vers1.equals(vers2);
  }
}
