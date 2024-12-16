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
package jp.ecuacion.splib.web.service;

import java.util.Optional;
import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.lib.core.exception.checked.BizLogicAppException;
import jp.ecuacion.lib.jpa.entity.AbstractEntity;
import jp.ecuacion.splib.jpa.repository.SplibRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

public interface SplibJpaServiceInterface<E extends AbstractEntity> {

  /** findAndOptimisticLockingCheck() にて使用。selectのためのrepositoryを取得。 */
  public abstract SplibRepository<E, Long> getRepositoryForOptimisticLocking();

  /**
   * findAndOptimisticLockingCheck()にて使用。楽観的排他制御のためのversionを取得。
   * {@link #findAndOptimisticLockingCheck(String, String...)}を参照
   */
  public abstract String[] getVersionsForOptimisticLocking(E e);

  /**
   * 一覧画面から単票画面への遷移・編集時・削除時などで、楽観的排他制御チェックを行う処理。併せてDBからentityを取得する。
   * 本メソッド内で、あらかじめ画面表示の際に取得しhtml内に埋めたversionと別途本処理の中で取得したentityのversion比較を行う。
   * 現時点での差異確認は行うが、その後目的を達成するまでには、もう一度楽観的チェックが行われるので、必ず本メソッドの戻り値として取得するentityを後続で使用すること。
   * （たとえばデータupdate時は、本チェックでversionに差異がないことを確認した後、update処理実施までに他のユーザからupdateされた場合もエラーとする必要があるため。）
   * 
   * <p>
   * 比較するversionは、1テーブルの場合は1つのversionを普通に格納すれば良いのだが、
   * 複数テーブルを一度に更新する場合などは、そのそれぞれのテーブルのversionを比較する必要が出る。それも踏まえて配列形式でversionを保持している。
   * （それぞれのstring要素の中に、カンマ区切りなどで複数の要素を持つこともできるのだが、区切り文字などを気にしなくて良いように配列で持っている。
   * 　二次元配列でないと必要なversionを保持できないような場合は、カンマ区切りなどで複数の値を一つの要素に入れること。）
   * </p>
   * 
   * <p>
   * その場合、画面上では複数の値を持つのでそれらをString配列に入れ、DBから取得するersionの値も同じ形にしてstring配列で比較を行う。
   * 尚、どのテーブルを更新するか否かに関わらず、どれかひとつでもversionにずれがあればエラーとする仕様。
   * そもそも頻度が高いわけではないので、他のテーブルだろうと更新があったら確認し直してから行更新してくれ、と。
   * </p>
   * 
   * <p>
   * relationのないテーブルも同時に更新する場合などは、getVersionsForOptimisticLocking(E e) だけでは情報不足なので、
   * 呼ばれた先でさらに値を取得する必要がある。その追加取得したentityも、後続処理で使用する必要があるため注意。
   * </p>
   */
  public default E findAndOptimisticLockingCheck(String idOfRootRecord,
      String... versionsInScreen) throws AppException {
    Optional<E> optional =
        getRepositoryForOptimisticLocking().findById(Long.valueOf(idOfRootRecord));

    // 他のユーザがレコードを削除してしまい、削除フラグが立って検索できない状態になる場合がある。
    // それを見越して、指定のIDのデータが存在しない場合も排他制御エラーとする。
    if (optional.isEmpty()) {
      String msg = "jp.ecuacion.splib.web.common.message.sameRecordAlreadyDeleted";
      throw new BizLogicAppException(msg);
    }

    E e = optional.get();

    // versionが異なる場合は楽観的排他制御エラーとする
    if (!isVersionsTheSame(versionsInScreen, getVersionsForOptimisticLocking(e))) {
      throw new ObjectOptimisticLockingFailureException("some class", idOfRootRecord);
    }

    return e;
  }

  private boolean isVersionsTheSame(String[] vers1, String[] vers2) {
    // nullは不可とする。削除などにより値が無くなったのであれば、長さゼロの配列で。
    if (vers1 == null || vers2 == null) {
      throw new RuntimeException(
          "Optimistic Locking check cannot be done because version array is null.");
    }
    
    if (vers1.length != vers2.length) {
      return false;
    }

    for (int i = 0; i < vers1.length; i++) {
      if (!vers1[i].equals(vers2[i])) {
        return false;
      }
    }

    return true;
  }
}
