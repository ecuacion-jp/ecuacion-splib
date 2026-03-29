# Breaking Change: components.html の分割

## 概要

`ecuacion-splib-web` の `bootstrap/components.html` から、以下のファイルにフラグメントが分離されました。

| 新ファイル | 収録内容 |
|---|---|
| `bootstrap/components-list.html` | リスト・テーブル関連（th/td系、ページング） |
| `bootstrap/components-search.html` | 検索コンポーネント（search系） |
| `bootstrap/components-edit.html` | 編集ページボタン（edit-page button） |
| `bootstrap/components-input.html` | 入力コンポーネント（input系）、submitButton |

`bootstrap/components.html` を参照しているプロジェクトは、
以下に記載する **対応フラグメントの参照先を変更する必要があります**。

---

## Claudeへの作業指示

このドキュメントを読んだClaudeは、以下の手順を自律的に実行してください。

### 手順1: 対象ファイルを検索する

プロジェクト内のすべての `.html` ファイルで、次のパターンを検索してください:

```
bootstrap/components ::
```

### 手順2: 参照先を変更する

検索結果の中に、以下の **移動済みフラグメント名** が含まれる行があれば、
対応する新ファイルへ参照先を変更してください。

#### `bootstrap/components-list` へ変更するフラグメント

| フラグメント名 |
|---|
| `sortPagingAndRecordsInScreen` |
| `scriptForThSortable` |
| `scriptForRecordsInScreen` |
| `scriptForPagenation` |
| `scriptForSortPagingAndRecordsInScreen` |
| `thText` |
| `thSortable` |
| `thCustomizable` |
| `thFreeText` |
| `thUpdateButton` |
| `thDeleteButton` |
| `thDownloadButton` |
| `thSwitchUserButton` |
| `tdReadOnlyCustomizable` |
| `tdReadOnlyText` |
| `tdReadOnlyNumber` |
| `tdReadOnlyFixedLength` |
| `tdReadOnlyFreeText` |
| `tdReadOnlyFreeFixedLength` |
| `tdReadOnlyEnumText` |
| `tdReadOnlyCheckbox` |
| `internalTdInputInsideTdCommon` |
| `internalTdInputCommon` |
| `tdInputText` |
| `tdInputNumber` |
| `tdInputHidden` |
| `tdInputCheckbox` |
| `showInsertFormButton` |
| `tdScriptButton` |
| `tdSubmitButton` |
| `tdSubmitButtonWithIds` |
| `tdUpdateButton` |
| `tdDeleteButton` |
| `tdDownloadButton` |
| `tdSwitchUserButton` |
| `scriptForTdSubmitButtonWithIds` |
| `scriptForTdSwitchUserButton` |

#### `bootstrap/components-input` へ変更するフラグメント

| フラグメント名 |
|---|
| `internalAllInputCommonComponent` |
| `internalAllHtmlInputComponent` |
| `internalAllHtmlInputNumberComponent` |
| `internalAllHtmlSelectComponent` |
| `internalAllHtmlCheckboxComponent` |
| `internalEditInputCommonComponent` |
| `internalEditInputTextComponent` |
| `inputText` |
| `inputPassword` |
| `inputHidden` |
| `inputNumber` |
| `internalEditInputDateTime` |
| `inputYearMonth` |
| `inputDate` |
| `inputTime` |
| `inputDateTime` |
| `inputTextArea` |
| `inputDocumentViewer` |
| `inputCheckbox` |
| `inputSwitch` |
| `inputCheckboxes` |
| `internalEditInputSelectComponent` |
| `inputSelect` |
| `inputSelectWithLocale` |
| `inputSelectFromEnum` |
| `inputFile` |
| `inputTakenPhotoMobile` |
| `inputTakenPhotoPc` |
| `inputCaptcha` |
| `submitButton` |

#### `bootstrap/components-edit` へ変更するフラグメント

| フラグメント名 |
|---|
| `insertOrUpdateButton` |
| `backButton` |
| `downloadButton` |

#### `bootstrap/components-search` へ変更するフラグメント

| フラグメント名 |
|---|
| `internalSearchInputCommonComponent` |
| `searchText` |
| `searchNumber` |
| `searchDate` |
| `searchComponentSelect` |
| `searchSelect` |
| `searchSelectFromEnum` |
| `searchSelectFromBoolean` |
| `searchButton` |
| `searchConditionClearButton` |

#### `bootstrap/components-general-form` へ変更するフラグメント

| フラグメント名 |
|---|
| `generalForm` |

### 手順3: 変更例

**変更前:**
```html
<div th:replace="~{bootstrap/components :: inputText('fieldName', '')}"></div>
<div th:replace="~{bootstrap/components :: submitButton('action', 'label.key', '')}"></div>
<div th:replace="~{bootstrap/components :: thText('fieldName', '')}"></div>
<div th:replace="~{bootstrap/components :: tdUpdateButton('')}"></div>
<div th:replace="~{bootstrap/components :: scriptForSortPagingAndRecordsInScreen('')}"></div>
<div th:replace="~{bootstrap/components :: searchButton('')}"></div>
<div th:replace="~{bootstrap/components :: searchText('fieldName', '')}"></div>
<div th:replace="~{bootstrap/components :: insertOrUpdateButton(${isInsert}, '')}"></div>
<div th:replace="~{bootstrap/components :: backButton('')}"></div>
<div th:replace="~{bootstrap/components :: generalForm(~{::formContent}, ~{}, ~{}, '', false, '', '')}"></div>
```

**変更後:**
```html
<div th:replace="~{bootstrap/components-input :: inputText('fieldName', '')}"></div>
<div th:replace="~{bootstrap/components-input :: submitButton('action', 'label.key', '')}"></div>
<div th:replace="~{bootstrap/components-list :: thText('fieldName', '')}"></div>
<div th:replace="~{bootstrap/components-list :: tdUpdateButton('')}"></div>
<div th:replace="~{bootstrap/components-list :: scriptForSortPagingAndRecordsInScreen('')}"></div>
<div th:replace="~{bootstrap/components-search :: searchButton('')}"></div>
<div th:replace="~{bootstrap/components-search :: searchText('fieldName', '')}"></div>
<div th:replace="~{bootstrap/components-edit :: insertOrUpdateButton(${isInsert}, '')}"></div>
<div th:replace="~{bootstrap/components-edit :: backButton('')}"></div>
<div th:replace="~{bootstrap/components-general-form :: generalForm(~{::formContent}, ~{}, ~{}, '', false, '', '')}"></div>
```

### 手順4: 変更対象外のフラグメント

`bootstrap/components ::` の参照であっても、上記一覧にないフラグメント名は
**変更不要**です（`bootstrap/components.html` に残っています）。

例（変更不要）:
```html
<div th:replace="~{bootstrap/components :: submitButton(...)}"></div>
<div th:replace="~{bootstrap/components :: inputText(...)}"></div>
<div th:replace="~{bootstrap/components :: navbarRoot(...)}"></div>
```

### 手順5: ビルドして確認する

変更後、ビルドが通ることを確認してください。
