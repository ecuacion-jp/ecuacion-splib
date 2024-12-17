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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import jp.ecuacion.splib.core.form.record.SplibRecord;
import jp.ecuacion.splib.web.form.SplibListForm;
import jp.ecuacion.splib.web.form.SplibSearchForm;
import org.springframework.security.core.userdetails.UserDetails;

//@formatter:off
public abstract class SplibSearchListService
    <FST extends SplibSearchForm, FLT extends SplibListForm<?>>
    extends SplibGeneral2FormsService<FST, FLT> {
  //@formatter:on

  /**
   * 以下の3つの処理を行う。
   * 
   * <p>
   * 1.検索条件適用結果の全件数取得と設定<br>
   * 2.ページ番号を踏まえた表示対象レコードデータの取得と設定<br>
   * 3.楽観的排他制御のための情報設定<br>
   * </p>
   * DBのように、外部でフィルタができる仕組みがある場合は1.と2.を別処理にできるが、
   * フィルタ含めてjava内で実施する場合は1.のフィルタ実施のために全件取得、1.で取得・フィルタした結果のlistを2.で使う形となる。
   */

  public abstract void delete(FLT listForm, UserDetails loginUser) throws Exception;

  protected List<? extends SplibRecord> getSortedList(List<? extends SplibRecord> listToSort,
      SplibSearchForm searchForm) {
    return getSortedList(listToSort, searchForm, new String[] {});
  }

  protected List<? extends SplibRecord> getSortedList(List<? extends SplibRecord> listToSort,
      SplibSearchForm searchForm, String[] needsNumberSortItems) {

    String itemId = searchForm.getSortItemWithDefault();
    boolean isDesc = searchForm.getDirection().equals(SplibSearchForm.DIRECTION_DESC);

    int directionVal = isDesc ? -1 : 1;

    if (Arrays.asList(needsNumberSortItems).contains(itemId)) {
      return listToSort.stream()
          .sorted((rec1, rec2) -> directionVal * (Integer.valueOf((String) rec1.getValue(itemId))
              .compareTo(Integer.valueOf((String) rec2.getValue(itemId)))))
          .toList();

    } else {
      return listToSort.stream()
          // #658: rec1.getValue(itemId) がnullの場合を考慮
          .sorted((rec1, rec2) -> rec1.getValue(itemId) == null ? -1 * directionVal
              : directionVal
                  * ((String) rec1.getValue(itemId)).compareTo((String) rec2.getValue(itemId)))
          .toList();
    }
  }

  protected List<? extends SplibRecord> getFilteredList(List<? extends SplibRecord> sortedList,
      SplibSearchForm seForm) {

    // 画面での指定件数が全件数を下回る場合は、対象のみを抜き出したリストとする
    Objects.requireNonNull(sortedList);
    List<? extends SplibRecord> finalList = new ArrayList<>(sortedList);
    if (sortedList.size() > seForm.getRecordsInScreen()) {
      for (int i = 0; i < sortedList.size(); i++) {
        if (!(seForm.getPage() * seForm.getRecordsInScreen() <= i
            && i <= (seForm.getPage() + 1) * seForm.getRecordsInScreen() - 1)) {
          finalList.remove(sortedList.get(i));
        }
      }
    }

    return finalList;
  }
}
