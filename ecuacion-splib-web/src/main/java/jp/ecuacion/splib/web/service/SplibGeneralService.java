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

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import jp.ecuacion.lib.core.exception.checked.AppWarningException;
import jp.ecuacion.splib.core.container.DatetimeFormatParameters;
import jp.ecuacion.splib.web.form.SplibGeneralForm;
import jp.ecuacion.splib.web.util.SplibUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;

public abstract class SplibGeneralService {

  @Autowired
  private HttpServletRequest request;

  /* ------- */
  /* warning */
  /* ------- */

  protected void throwWarning(Set<String> confirmedWarningMessageSet, Locale locale, String msgId)
      throws AppWarningException {
    if (!confirmedWarningMessageSet.contains(msgId)) {
      throw new AppWarningException(locale, msgId);
    }
  }

  /**  */
  protected void throwWarning(Set<String> confirmedWarningMessageSet, Locale locale,
      String buttonName, String msgId, String... params) throws AppWarningException {
    if (!confirmedWarningMessageSet.contains(msgId)) {
      throw new AppWarningException(locale, buttonName, null, msgId, params);
    }
  }

  /* ---------------------- */
  /* exclusive lock by file */
  /* ---------------------- */

  /**
   * lock fileの最終更新時刻を取得。楽観的排他制御を行うために使用。
   *
   * @return ファイルの更新時刻を文字列形式で保持。
   *         yyyy-mm-dd-hh-mi-ss.SSSの形式とし、timezoneが違う場所での処理でも問題にならないよう、常にUTCでの時刻とする。
   */
  protected String getLockFileVersion(File lockFile) throws IOException {
    // ディレクトリが存在しなければ作成
    lockFile.getParentFile().mkdirs();

    // ファイルが存在しなければ作成
    if (!lockFile.exists()) {
      lockFile.createNewFile();
    }

    return Instant.ofEpochMilli(lockFile.lastModified()).atOffset(ZoneOffset.ofHours(0))
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss.SSS"));
  }

  /**
   * lock fileによる悲観的排他制御を実施したい場合に使用できるutility method。
   * 
   * <p>
   * lockFileとして使用するpathを指定したFileオブジェクトを渡せば、あとは諸々やってくれる。 exclusiveLockActivatedByLockFile()
   * は、abstractにして実装必須にすると面倒（fileによるロックの頻度は高くない）なので、
   * 本クラスにて処理なしの通常メソッドとして実装しておき、使用する場合はそれをoverride、とする。
   * </p>
   *
   * @param version ファイルの更新時刻を文字列形式で保持。getLockFileVersion(File lockFile)の戻り値の形式。
   */
  protected void fileLock(File lockFile, String version, SplibGeneralForm form) throws Exception {
    FileLock lockedObject = null;
    try (FileChannel channel =
        FileChannel.open(lockFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);) {

      // ファイルロックを試行。例外発生ではなくlockedObjectがnullの場合は、ロック取得に失敗したことになる場合もあるらしい
      lockedObject = channel.tryLock();
      if (lockedObject == null) {
        throw new OverlappingFileLockException();
      }

      // 画面表示時のtimestampとの差異比較
      String fileTimestamp = getLockFileVersion(lockFile);
      if (!version.equals(fileTimestamp)) {
        throw new OverlappingFileLockException();
      }

      // ここまででロックを獲得できたので、ロックを獲得できた場合に実行したい内容を記載。
      exclusiveLockActivatedByLockFile(lockFile, form);

      // 特に使用はしないのだが、lockFileを更新する目的でtimestampの文字列を書き込んでおく。
      ByteBuffer src = ByteBuffer.allocate(30);
      byte[] bytes = LocalDateTime.now().toString().getBytes();
      src.put(bytes);
      src.position(0);

      channel.write(src);

      lockedObject.release();

    } catch (IOException | OverlappingFileLockException ex) {
      throw new OverlappingFileLockException();
    }
  }

  /**
   * ロックを獲得できた場合に実行したい内容を記載。 file lockを使用する場合は本メソッドをoverride。
   * 子クラスで実装必須になると面倒（ファイルロック使用時以外は使用しないので）なことからabstractにはしない。
   */
  protected void exclusiveLockActivatedByLockFile(File lockFile, SplibGeneralForm form)
      throws Exception {

  }

  /* ------ */
  /* record */
  /* ------ */

  /**
   * record内のlocalDate項目（String）をLocalDate形式で取得。
   */
  protected LocalDate localDate(String date) {
    return (date == null || date.equals("")) ? null
        : LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
  }

  /**
   * offsetはlogin画面でのonload時に呼ばれるため、login画面を開いた状態で放置した場合は値がnullでエラーになる。
   */
  public DatetimeFormatParameters getParams() {
    return new SplibUtil().getParams(request);
  }

  /** SplibGeneralServivceでは、メソッドは定義しておくものの中身は実装しない。 */
  public void prepareForm(List<SplibGeneralForm> allFormList, UserDetails loginUser) {
    System.out
        .println("prepareForm(List<SplibGeneralForm> allFormList, UserDetails loginUser) called.");
  }

  /* ----- */
  /* other */
  /* ----- */

}
