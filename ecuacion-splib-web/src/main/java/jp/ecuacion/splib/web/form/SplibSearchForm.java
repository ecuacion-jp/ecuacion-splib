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
import jp.ecuacion.lib.core.util.PropertyFileUtil;
import jp.ecuacion.splib.web.form.record.RecordInterface;
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
   * 検索・一覧表示画面で、検索条件を指定して検索した際の検索条件は保管しておきたい.
   * 
   *  <p>一方で、メニューのリンクなど、検索条件を載せない形で画面にアクセスした際は、
   * その検索条件を保管するのではなく保存済みの検索条件で検索したい。 この分岐を本フラグで対応。</p>
   */
  private boolean requestFromSearchForm;

  /**
   * Specifies whether the form is prepared, which means "prepareForm" method is applied.
   */
  private boolean prepared = false;

  // 検索に必要な条件
  protected String sortItem;
  protected String direction;
  protected Integer page;
  protected Integer recordsInScreen;

  /** pagerを作成するのに必要となるため、serviceから受け取りpager作成時に使用. */
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

  /** こちらはdefaultで設定をしておくが個別form毎に変更が可能. */
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
    return (direction != null && getDirection().equals(DIRECTION_DESC)) ? Direction.DESC
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
   * 値のセットはsetNumberOfRecordsAndAdjustCurrentPageNumger()を使用する前提であり、
   * これでない方法でnumberOfRecordsを設定すると処理が正しく動かないためあえて通常のsetterは削除した.
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

    // ページ調整
    changePageIfThePageNumberExceedsTheLast();
  }

  /**
   * 最終ページを超えた状態だと変になるので、件数が存在する最終ページより後のページを示している場合は最終ページに変更.
   */
  private void changePageIfThePageNumberExceedsTheLast() {

    // 件数から考えられる最終ページ（Pagerベースのゼロから始まるページ番号）
    int lastPageFromRecordCount =
        numberOfRecords / recordsInScreen + (numberOfRecords % recordsInScreen > 0 ? 1 : 0) - 1;

    // 上記ロジックだと、検索結果が0件の場合にlastPageFromRecordCount == -1となるため補正
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
   * pagerを作成するためのPagerInfoのリストを生成.
   * 
   * <p>Pager作成のルールは以下。<br>
   * </p>
   * <ul>
   * <li>レコード件数が0件及び、現在のrecordsInScreen以下の場合はpagerを表示しない</li>
   * <li>上記条件より、pagerが存在する際は常に2ページ以上存在することとなる。</li>
   * <li>次ページ・全ページへ移動する「Previous」「Next」は（Pagerが存在する限り）常に存在するものとし、
   * 現在の表示が1ページ目の場合はPreviousは押せない、現ページが最終ページ（以降）の場合はNextは押せない、という制御とする
   * （「現ページが最終ページ（以降）」と「以降」をつけているのは、</li>
   * <li>元々50レコードあり、5件表示で10ページまで表示していたが、画面表示後にレコードを30件削除、
   * その後画面から10ページ目のボタンを押した場合、素直にPageRequestすると、10ページ目を取得しようとし0件取得になるのだが、
   * エラーになるわけではないし頻繁にあることではないのでそれでよしとする、つまりpager上も10ページを最終ページとし表示する。
   * その状態でPreviousを押すと、これまたデータが存在しない9ページ目を表示しようとするがそれもOKとする。
   * （そもそも一気にデータが消されるのは通常オペレーションでは考えにくいと思われるし、一旦1ページ目に戻りpagerを見れば件数が減ったとわかるので）<br>
   * →表示件数や検索条件を同時に変更すると、割と頻発する状態になったため、最終ページより後に現在ページがある場合は最終ページに変更する処理を追加。
   * （setNumberOfRecordsAndAdjustCurrentPageNumger() -> changePageIfThePageNumberExceedsTheLast()
   * のところ） 本処理のロジックは現時点ではそのままとしておく。間違いなく不要であることを確認後必要ならその部分のロジックを削除可。</li>
   * <li>前ページ・次ページに移動する手段は「Previous」「Next」を使用し、ページ番号で表示されるセルは、現ページ、1ページ、最終ページのみとする。
   * （次のページの番号のリンクとNextを両方出すのは冗長なので）</li>
   * <li>現ページの隣のページが1ページ目ないし最終ページでない場合は、間に押下不可な「...」のセルを入れる</li>
   * <li>つまり、押下不可を(*)で表すと、pagerでは以下のように表示される。
   * <ul>
   * <li>全2ページ・現在1ページの場合：Previous(*)|1|2|Next(*)</li>
   * <li>全3ページ・現在1ページの場合：Previous(*)|1|...(*)|3|Next</li>
   * <li>全3ページ・現在2ページの場合：Previous|1|2|3|Next</li>
   * <li>全4ページ・現在1ページの場合：Previous(*)|1|...(*)|4|Next</li>
   * <li>全4ページ・現在2ページの場合：Previous|1|2|...(*)|4|Next</li>
   * <li>全5ページ・現在3ページの場合：Previous|1|...(*)|3|...(*)|5|Next</li>
   * </ul>
   * </li>
   * </ul>
   */
  public List<PagerInfo> getPagerInfoList(Locale locale) {
    final String labelPrev =
        PropertyFileUtil.getMessage(locale, "jp.ecuacion.splib.web.common.label.prev");
    final String labelNext =
        PropertyFileUtil.getMessage(locale, "jp.ecuacion.splib.web.common.label.next");

    List<PagerInfo> rtnList = new ArrayList<>();
    PageRequest pageRequest = getPageRequest();

    // pager非表示の場合は、ゼロ件listで返す
    if (numberOfRecords <= recordsInScreen) {
      return rtnList;
    }

    // まずPreviousセルを生成。
    // 現ページが2ページ目であればPreviousが押せるのでそこで分岐。
    rtnList.add(pageRequest.getPageNumber() > 0
        ? new PagerInfo(labelPrev, false, pageRequest.getPageNumber() - 1)
        : new PagerInfo(labelPrev));

    // 次は必ず1。現ページが1ならactive, それ以外なら""なので、disabledであることはない。
    rtnList.add(new PagerInfo("1", pageRequest.getPageNumber() == 0, 0));

    // 次に「...」がくる場合は、現ページが1で全3ページ以上ある場合、または現ページが3以降の場合。
    if ((pageRequest.getPageNumber() == 0 && numberOfRecords > recordsInScreen * 2)
        || pageRequest.getPageNumber() >= 2) {
      rtnList.add(new PagerInfo("..."));
    }

    // 件数から考えられる最終ページ（Pagerベースのゼロから始まるページ番号）
    int lastPageFromRecordCount =
        numberOfRecords / recordsInScreen + (numberOfRecords % recordsInScreen > 0 ? 1 : 0) - 1;

    // 次は、1ページ目と最終ページに挟まれる数字。現ページが1ページ目または最終ページの場合は表示されないのでスキップ
    boolean needsSandwichedNumber =
        pageRequest.getPageNumber() != 0 && pageRequest.getPageNumber() < lastPageFromRecordCount;
    if (needsSandwichedNumber) {
      // 挟まれるページは、必ずactive
      rtnList.add(new PagerInfo(Integer.toString(pageRequest.getPageNumber() + 1), true,
          pageRequest.getPageNumber()));
    }

    // 後ろ側の「...」。現ページが最終ページより2ページ以上前の場合。
    // ただ、これだけだと「Previous|1|...|...|5|Next」という表記になってしまうため、
    // さらに「挟まれる数字がある場合」に限定して表示。
    if (pageRequest.getPageNumber() <= lastPageFromRecordCount - 2 && needsSandwichedNumber) {
      rtnList.add(new PagerInfo("..."));
    }

    // Pagerの最終ページ（Pagerベースのゼロから始まるページ番号）。
    // 5件表示、10ページあったが、画面表示後に全件削除、その後10ページ目をクリックした場合は、
    // レコード件数上は存在しないが、Pager上は10ページ目まである（現在10ページ）として返す。
    // それを踏まえた最終ページ番号。
    int lastPage = lastPageFromRecordCount > pageRequest.getPageNumber() ? lastPageFromRecordCount
        : pageRequest.getPageNumber();

    // 最終ページ
    rtnList.add(new PagerInfo(Integer.toString(lastPage + 1),
        pageRequest.getPageNumber() == lastPage, lastPage));

    // Nextセル。
    // 現ページが最終ページ以外であれば押せる。
    rtnList.add(pageRequest.getPageNumber() != lastPage
        ? new PagerInfo(labelNext, false, pageRequest.getPageNumber() + 1)
        : new PagerInfo(labelNext));

    return rtnList;
  }

  /**
   * Is used for search result records display like "6-10 / 15".
   */
  public String getLinesInScreen() {
    // ゼロ件の場合
    if (numberOfRecords == 0) {
      return "";

      // 不具合で、最終ページ以降を表示してしまった場合
      // （修正する予定だがもしなってもエラーにならないように実装しておく）
    } else if (numberOfRecords / recordsInScreen < page) {
      return "( - / " + numberOfRecords + ")";
    }

    int min = page * recordsInScreen + 1;
    int normalMax = (page + 1) * recordsInScreen;
    int max = normalMax > numberOfRecords ? numberOfRecords : normalMax;
    return "( " + min + " - " + max + " / " + numberOfRecords + " )";
  }

  @Override
  protected List<String> getNotEmptyFields(RecordInterface rootRecord, String loginState,
      RolesAndAuthoritiesBean bean) {
    return rootRecord.getNotEmptyOnSearchFields(loginState, bean);
  }
}
