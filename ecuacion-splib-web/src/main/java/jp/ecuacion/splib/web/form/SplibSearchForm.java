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
package jp.ecuacion.splib.web.form;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import jp.ecuacion.lib.core.util.PropertiesFileUtil;
import jp.ecuacion.splib.web.item.HtmlItemContainer;
import jp.ecuacion.splib.web.util.SplibSecurityUtil.RolesAndAuthoritiesBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

/**
 * Stores data for search.
 */
public abstract class SplibSearchForm extends SplibGeneralForm {
  public static final String DIRECTION_ASC = "asc";
  public static final String DIRECTION_DESC = "desc";

  /**
   * On the search and list screen, search conditions specified by the user should be retained.
   *
   * <p>On the other hand, when the screen is accessed without search conditions
   * (e.g. from a menu link), it is preferable to search using the previously saved conditions
   * rather than saving the empty conditions. This flag handles that branching.</p>
   */
  private boolean requestFromSearchForm;

  /**
   * Expresses whether the form is just created with no search conditions.
   * 
   * <p>Only when this is true, you should set initial search condition at your
   *     searchListService#page().</p>
   * 
   * <p>This is null right after the first constructed with default initial condition.
   *     In SplibSearchListController#getProperSearchForm(Model, FST) changes it to "true".
   *     Next time this passes the method above, it again changes to "false".</p>
   */
  private Boolean newlyCreated;

  /**
   * Specifies whether the form is prepared, which means "prepareForm" method is applied.
   */
  private boolean prepared = false;

  // Fields needed for search.
  protected String sortItem;
  protected String direction;
  protected Integer page;
  protected Integer recordsInScreen;

  /** Received from service and used when creating the pager. */
  protected Integer numberOfRecords;

  public boolean isPrepared() {
    return prepared;
  }

  public void setPrepared(boolean prepared) {
    this.prepared = prepared;
  }

  /**
   * Gets getDefaultSortItem.
   * 
   * @return String
   */
  @Nonnull
  protected abstract String getDefaultSortItem();

  /** Default value is set here but can be changed per individual form. */
  protected String getDefaultDirection() {
    return DIRECTION_ASC;
  }

  /**
   * Constructs a new instance.
   */
  public SplibSearchForm() {
    // Set default values.
    this.sortItem = getDefaultSortItem();
    this.direction = getDefaultDirection();
    this.page = 0;
    this.recordsInScreen = 5;
  }

  public boolean isRequestFromSearchForm() {
    return requestFromSearchForm;
  }

  public void setRequestFromSearchForm(boolean requestFromSearchForm) {
    this.requestFromSearchForm = requestFromSearchForm;
  }

  /** 
   * Returns true when the search conditions in the form is newly created 
   *     and it's the right time to set initial conditions at searchListService#page().
   */
  public boolean getNewlyCreated() {
    return newlyCreated == null ? false : newlyCreated;
  }

  /** 
   * "getNewlyCreated()" does not return null to simplify its usage from general users.
   * Otherwise the code inside want to know if the value is null, 
   * so the method obtaining raw value (means it may return null) prepared.
   */
  public Boolean getNewlyCreatedRawValue() {
    return newlyCreated;
  }

  public void setNewlyCreated(Boolean newlyCreated) {
    this.newlyCreated = newlyCreated;
  }

  public String getSortItem() {
    return sortItem;
  }

  /**
   * Gets SortItemWithDefault.
   * 
   * @return String
   */
  @Nonnull
  public String getSortItemWithDefault() {
    String rtn = (sortItem == null) ? getDefaultSortItem() : sortItem;
    return Objects.requireNonNull(rtn);
  }

  public void setSortItem(String sortItem) {
    this.sortItem = sortItem;
  }

  public String getDirection() {
    return direction;
  }

  public Sort.Direction getDirectionEnum() {
    return (direction != null && direction.equals(DIRECTION_DESC)) ? Direction.DESC
        : Direction.ASC;

  }

  public void setDirection(String direction) {
    this.direction = direction;
  }

  /**
   * Returns next direction.
   * 
   * @param sortItem sortItem
   * @return String
   */
  public String nextDirection(String sortItem) {
    if (this.sortItem != null && this.sortItem.equals(sortItem)
        && getDirection().equals(DIRECTION_ASC)) {
      return DIRECTION_DESC;

    } else {
      return DIRECTION_ASC;
    }
  }

  public Integer getPage() {
    return page;
  }

  public void setPage(Integer page) {
    this.page = page;
  }

  public Integer getRecordsInScreen() {
    return recordsInScreen;
  }

  public void setRecordsInScreen(Integer recordsInScreen) {
    this.recordsInScreen = recordsInScreen;
  }

  /**
   * The value is assumed to be set via setNumberOfRecordsAndAdjustCurrentPageNumger().
   * If numberOfRecords is set by any other means the processing will not work correctly,
   * so the normal setter has been intentionally removed.
   */
  public Integer getNumberOfRecords() {
    return numberOfRecords;
  }

  /**
   * Sets number of records and adjust current page number.
   * 
   * @param numberOfRecords numberOfRecords
   */
  public void setNumberOfRecordsAndAdjustCurrentPageNumger(Long numberOfRecords) {
    this.numberOfRecords = numberOfRecords.intValue();

    // Page adjustment.
    changePageIfThePageNumberExceedsTheLast();
  }

  /**
   * Changes to the last page if the current page exceeds the last page that has records,
   * since going beyond the last page causes unexpected behavior.
   */
  private void changePageIfThePageNumberExceedsTheLast() {

    // Last page derived from record count (zero-based page number used by Pager).
    int lastPageFromRecordCount =
        numberOfRecords / recordsInScreen + (numberOfRecords % recordsInScreen > 0 ? 1 : 0) - 1;

    // Correct: when search result is 0 records, lastPageFromRecordCount becomes -1.
    if (lastPageFromRecordCount < 0) {
      lastPageFromRecordCount = 0;
    }

    if (getPage() > lastPageFromRecordCount) {
      page = lastPageFromRecordCount;
    }
  }

  public PageRequest getPageRequest() {
    return PageRequest.of(getPage(), getRecordsInScreen(),
        Sort.by(new Sort.Order(getDirectionEnum(), getSortItemWithDefault())));
  }

  /**
   * Generates the list of PagerInfo to build the pager.
   *
   * <p>Rules for building the pager:<br>
   * </p>
   * <ul>
   * <li>If the record count is 0 or is at most recordsInScreen, the pager is not shown.</li>
   * <li>From the condition above, when the pager is shown there are always 2 or more pages.</li>
   * <li>"Previous" and "Next" cells are always present as long as the pager exists.
   * When on page 1 Previous is disabled; when on the last page (or beyond) Next is disabled.
   * (The "or beyond" qualifier is added because:</li>
   * <li>Originally 50 records existed shown 5 per page (10 pages), but after displaying the
   * screen 30 records were deleted. If the user then clicks page 10, PageRequest tries to fetch
   * page 10 and returns 0 records — but since this is not an error and is infrequent, it is
   * accepted and page 10 is shown as the last page in the pager.
   * If Previous is clicked in that state, page 9 (also with no data) is shown, and that
   * is also accepted. (Sudden mass deletion is unlikely in normal operation, and navigating
   * back to page 1 will show that the count decreased.)<br>
   * NOTE: When display count or search conditions are changed simultaneously this situation
   * occurs fairly often, so a process was added to change the current page to the last page
   * when it exceeds the last page.
   * (setNumberOfRecordsAndAdjustCurrentPageNumger()
   * -&gt; changePageIfThePageNumberExceedsTheLast())
   * The logic of this process is left as-is for now; it can be removed once confirmed
   * to be definitely unnecessary.</li>
   * <li>Navigation uses "Previous"/"Next", and only the current page, page 1, and the last
   * page are shown as numbered cells. (Showing the next page number link and Next would be
   * redundant.)</li>
   * <li>If the page adjacent to the current page is neither page 1 nor the last page, a
   * non-clickable "..." cell is inserted between them.</li>
   * <li>In short, denoting non-clickable cells with (*), the pager appears as follows:
   * <ul>
   * <li>2 total pages, current page 1: Previous(*)|1|2|Next(*)</li>
   * <li>3 total pages, current page 1: Previous(*)|1|...(*)|3|Next</li>
   * <li>3 total pages, current page 2: Previous|1|2|3|Next</li>
   * <li>4 total pages, current page 1: Previous(*)|1|...(*)|4|Next</li>
   * <li>4 total pages, current page 2: Previous|1|2|...(*)|4|Next</li>
   * <li>5 total pages, current page 3: Previous|1|...(*)|3|...(*)|5|Next</li>
   * </ul>
   * </li>
   * </ul>
   */
  public List<PagerInfo> getPagerInfoList(Locale locale) {
    final String labelPrev =
        PropertiesFileUtil.getMessage(locale, "jp.ecuacion.splib.web.common.label.prev");
    final String labelNext =
        PropertiesFileUtil.getMessage(locale, "jp.ecuacion.splib.web.common.label.next");

    List<PagerInfo> rtnList = new ArrayList<>();
    PageRequest pageRequest = getPageRequest();

    // Return empty list when pager is not shown.
    if (numberOfRecords <= recordsInScreen) {
      return rtnList;
    }

    // Generate the Previous cell first.
    // Previous is clickable when the current page is 2nd or later.
    rtnList.add(pageRequest.getPageNumber() > 0
        ? new PagerInfo(labelPrev, false, pageRequest.getPageNumber() - 1)
        : new PagerInfo(labelPrev));

    // Next cell is always 1. If current page is 1 it's active, otherwise ""; never disabled.
    rtnList.add(new PagerInfo("1", pageRequest.getPageNumber() == 0, 0));

    // The "..." cell comes next when: current page is 1 and there are 3+ total pages,
    // or current page is 3 or later.
    if ((pageRequest.getPageNumber() == 0 && numberOfRecords > recordsInScreen * 2)
        || pageRequest.getPageNumber() >= 2) {
      rtnList.add(new PagerInfo("..."));
    }

    // Last page derived from record count (zero-based page number used by Pager).
    int lastPageFromRecordCount =
        numberOfRecords / recordsInScreen + (numberOfRecords % recordsInScreen > 0 ? 1 : 0) - 1;

    // The number sandwiched between page 1 and the last page.
    // Skipped when current page is 1 or the last page.
    boolean needsSandwichedNumber =
        pageRequest.getPageNumber() != 0 && pageRequest.getPageNumber() < lastPageFromRecordCount;
    if (needsSandwichedNumber) {
      // The sandwiched page is always active.
      rtnList.add(new PagerInfo(Integer.toString(pageRequest.getPageNumber() + 1), true,
          pageRequest.getPageNumber()));
    }

    // Trailing "..." shown when current page is 2 or more pages before the last.
    // Without the additional condition this would render "Previous|1|...|...|5|Next",
    // so it is further limited to show only when a sandwiched number exists.
    if (pageRequest.getPageNumber() <= lastPageFromRecordCount - 2 && needsSandwichedNumber) {
      rtnList.add(new PagerInfo("..."));
    }

    // The last page in the Pager (zero-based page number used by Pager).
    // If 10 pages were shown (5 records each) but all records were deleted after display
    // and the user clicks page 10, the Pager returns page 10 as the last page
    // even though no records exist at that page number.
    int lastPage = lastPageFromRecordCount > pageRequest.getPageNumber() ? lastPageFromRecordCount
        : pageRequest.getPageNumber();

    // Last page.
    rtnList.add(new PagerInfo(Integer.toString(lastPage + 1),
        pageRequest.getPageNumber() == lastPage, lastPage));

    // Next cell.
    // Clickable when the current page is not the last page.
    rtnList.add(pageRequest.getPageNumber() != lastPage
        ? new PagerInfo(labelNext, false, pageRequest.getPageNumber() + 1)
        : new PagerInfo(labelNext));

    return rtnList;
  }

  /**
   * Is used for search result records display like "6-10 / 15".
   */
  public String getLinesInScreen() {
    // When there are zero records.
    if (numberOfRecords == 0) {
      return "";

      // When displaying beyond the last page due to a bug.
      // (Planned to fix, but implemented to avoid errors if this occurs.)
    } else if (numberOfRecords / recordsInScreen < page) {
      return "( - / " + numberOfRecords + ")";
    }

    int min = page * recordsInScreen + 1;
    int normalMax = (page + 1) * recordsInScreen;
    int max = normalMax > numberOfRecords ? numberOfRecords : normalMax;
    return "( " + min + " - " + max + " / " + numberOfRecords + " )";
  }

  @Override
  protected List<String> getNotEmptyItemPropertyPathList(HtmlItemContainer rootRecord,
      String loginState, RolesAndAuthoritiesBean bean) {
    return rootRecord.getNotEmptyOnSearchItemPropertyPathList(loginState, bean);
  }
}
