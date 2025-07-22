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

/**
 * Provides abstract search list service.
 * 
 * <p>In searching and list the results there are 3 procedures.</p>
 * 
 * <ol>
 * <li>To obtain the number of all the records adopting to the search conditions.</li>
 * <li>To obtain the the sorted records adopting to the search conditions 
 *     and the page number currently showing</li>
 * <li>To set the data for optimistic exclusive control</li>
 * </ol>
 * 
 * <p>In the case that storage has the function of data sorting and filtering (like DB),
 *     you should use those functions to ease the procedure.
 *     but in the case that storage doesn't have them, you need to sorting and filtering 
 *     in java code. In that case {@code getSortedList} sorts the data, 
 *     and {@code getFilteredList} filters them.</p>
 * 
 * @param <FST> SplibSearchForm
 * @param <FLT> SplibListForm
 */
//@formatter:off
public abstract class SplibSearchListService
    <FST extends SplibSearchForm, FLT extends SplibListForm<?>>
    extends SplibGeneral2FormsService<FST, FLT> {
  //@formatter:on
  
  /**
   * Deletes the record.
   * 
   * @param listForm listForm
   * @param loginUser loginUser
   * @throws Exception Exception
   */
  public abstract void delete(FLT listForm, UserDetails loginUser) throws Exception;

  /**
   * Sorts the list.
   * 
   * @param listToSort listToSort
   * @param searchForm searchForm
   * @return List
   */
  protected List<? extends SplibRecord> getSortedList(List<? extends SplibRecord> listToSort,
      SplibSearchForm searchForm) {
    return getSortedList(listToSort, searchForm, new String[] {});
  }

  /**
   * Sorts the list.
   * 
   * @param listToSort listToSort
   * @param searchForm searchForm
   * @param needsNumberSortItems needsNumberSortItems
   * @return List
   */
  protected List<? extends SplibRecord> getSortedList(List<? extends SplibRecord> listToSort,
      SplibSearchForm searchForm, String[] needsNumberSortItems) {

    String itemKindId = searchForm.getSortItemWithDefault();
    boolean isDesc = searchForm.getDirection().equals(SplibSearchForm.DIRECTION_DESC);

    int directionVal = isDesc ? -1 : 1;

    if (Arrays.asList(needsNumberSortItems).contains(itemKindId)) {
      return listToSort.stream()
          .sorted(
              (rec1, rec2) -> directionVal * (Integer.valueOf((String) rec1.getValue(itemKindId))
                  .compareTo(Integer.valueOf((String) rec2.getValue(itemKindId)))))
          .toList();

    } else {
      return listToSort.stream()
          // #658: rec1.getValue(itemKindId) がnullの場合を考慮
          .sorted((rec1, rec2) -> rec1.getValue(itemKindId) == null ? -1 * directionVal
              : directionVal * ((String) rec1.getValue(itemKindId))
                  .compareTo((String) rec2.getValue(itemKindId)))
          .toList();
    }
  }

  /**
   * Filters list.
   * 
   * @param sortedList sortedList
   * @param seForm seForm
   * @return list
   */
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
