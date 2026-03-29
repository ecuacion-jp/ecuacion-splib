# Breaking Change: components.html の分割

## 概要

`ecuacion-splib-web` の `bootstrap/components.html` から、以下のファイルにフラグメントが分離されました。

| 新ファイル | 収録内容 |
|---|---|
| `bootstrap/components-list.html` | リスト・テーブル関連（th/td系、ページング） |
| `bootstrap/components-search.html` | 検索コンポーネント（search系） |
| `bootstrap/components-edit.html` | 編集ページボタン（edit-page button） |
| `bootstrap/components-input.html` | 入力コンポーネント（input系）、submitButton |

また、`bootstrap/components-list.html` 内の一部フラグメントは **名前も変更** されています（`td` プレフィックスの削除）。

`bootstrap/components.html` または `bootstrap/components-list.html` を参照しているプロジェクトは、
以下に記載する対応を行ってください。

---

## Claudeへの作業指示

このドキュメントを読んだClaudeは、以下の手順を自律的に実行してください。

### 手順1: 対象ファイルを検索する

プロジェクト内のすべての `.html` ファイルで、次の **2つのパターン** を検索してください:

```
bootstrap/components ::
```

```
bootstrap/components-list ::
```

### 手順2: `bootstrap/components ::` の参照先を変更する

手順1の1つ目のパターンにヒットした行に、以下の **移動済みフラグメント名** が含まれていれば、
対応する新ファイルへ参照先を変更してください。
`bootstrap/components-list` への変更については、フラグメント名も併せて **新フラグメント名** に変更してください。

#### `bootstrap/components-list` へ変更するフラグメント

| 旧フラグメント名 | 新フラグメント名 |
|---|---|
| `sortPagingAndRecordsInScreen` | `sortPagingAndRecordsInScreen` |
| `scriptForThSortable` | `scriptForThSortable` |
| `scriptForRecordsInScreen` | `scriptForRecordsInScreen` |
| `scriptForPagenation` | `scriptForPagenation` |
| `scriptForSortPagingAndRecordsInScreen` | `scriptForSortPagingAndRecordsInScreen` |
| `thText` | `headerText` |
| `thSortable` | `headerSortable` |
| `thCustomizable` | `headerCustomizable` |
| `thFreeText` | `headerFreeText` |
| `thUpdateButton` | `headerUpdateButton` |
| `thDeleteButton` | `headerDeleteButton` |
| `thDownloadButton` | `headerDownloadButton` |
| `thSwitchUserButton` | `headerSwitchUserButton` |
| `tdReadOnlyCustomizable` | `readOnlyCustomizable` |
| `tdReadOnlyText` | `readOnlyText` |
| `tdReadOnlyNumber` | `readOnlyNumber` |
| `tdReadOnlyFixedLength` | `readOnlyFixedLength` |
| `tdReadOnlyFreeText` | `readOnlyFreeText` |
| `tdReadOnlyFreeFixedLength` | `readOnlyFreeFixedLength` |
| `tdReadOnlyEnumText` | `readOnlyEnumText` |
| `tdReadOnlyCheckbox` | `readOnlyCheckbox` |
| `internalTdInputInsideTdCommon` | `internalInsideCommon` |
| `internalTdInputCommon` | `internalCommon` |
| `tdInputText` | `text` |
| `tdInputNumber` | `number` |
| `tdInputHidden` | `hidden` |
| `tdInputCheckbox` | `checkbox` |
| `showInsertFormButton` | `showInsertFormButton` |
| `tdScriptButton` | `scriptButton` |
| `tdSubmitButton` | `submitButton` |
| `tdSubmitButtonWithIds` | `submitButtonWithIds` |
| `tdUpdateButton` | `updateButton` |
| `tdDeleteButton` | `deleteButton` |
| `tdDownloadButton` | `downloadButton` |
| `tdSwitchUserButton` | `switchUserButton` |
| `scriptForTdSubmitButtonWithIds` | `scriptForSubmitButtonWithIds` |
| `scriptForTdSwitchUserButton` | `scriptForSwitchUserButton` |

#### `bootstrap/components-input` へ変更するフラグメント

| 旧フラグメント名 | 新フラグメント名 |
|---|---|
| `internalAllInputCommonComponent` | `internalAllInputCommonComponent` |
| `internalAllHtmlInputComponent` | `internalAllHtmlInputComponent` |
| `internalAllHtmlInputNumberComponent` | `internalAllHtmlInputNumberComponent` |
| `internalAllHtmlSelectComponent` | `internalAllHtmlSelectComponent` |
| `internalAllHtmlCheckboxComponent` | `internalAllHtmlCheckboxComponent` |
| `internalEditInputCommonComponent` | `internalInputCommonComponent` |
| `internalEditInputTextComponent` | `internalEditInputTextComponent` |
| `inputText` | `text` |
| `inputPassword` | `password` |
| `inputHidden` | `hidden` |
| `inputNumber` | `number` |
| `internalEditInputDateTime` | `internalEditInputDateTime` |
| `inputYearMonth` | `yearMonth` |
| `inputDate` | `date` |
| `inputTime` | `time` |
| `inputDateTime` | `dateTime` |
| `inputTextArea` | `textArea` |
| `inputDocumentViewer` | `documentViewer` |
| `inputCheckbox` | `checkbox` |
| `inputSwitch` | `switch` |
| `inputCheckboxes` | `checkboxes` |
| `internalEditInputSelectComponent` | `internalSelectComponent` |
| `inputSelect` | `select` |
| `inputSelectWithLocale` | `selectWithLocale` |
| `inputSelectFromEnum` | `selectFromEnum` |
| `inputFile` | `file` |
| `inputTakenPhotoMobile` | `takenPhotoMobile` |
| `inputTakenPhotoPc` | `takenPhotoPc` |
| `inputCaptcha` | `captcha` |
| `submitButton` | `submitButton` |

#### `bootstrap/components-edit` へ変更するフラグメント

| フラグメント名 |
|---|
| `insertOrUpdateButton` |
| `backButton` |
| `downloadButton` |

#### `bootstrap/components-search` へ変更するフラグメント

| 旧フラグメント名 | 新フラグメント名 |
|---|---|
| `internalSearchInputCommonComponent` | `internalInputCommonComponent` |
| `searchText` | `text` |
| `searchNumber` | `number` |
| `searchDate` | `date` |
| `searchComponentSelect` | `componentSelect` |
| `searchSelect` | `select` |
| `searchSelectFromEnum` | `selectFromEnum` |
| `searchSelectFromBoolean` | `selectFromBoolean` |
| `searchButton` | `searchButton` |
| `searchConditionClearButton` | `searchConditionClearButton` |

#### `bootstrap/components-general-form` へ変更するフラグメント

| フラグメント名 |
|---|
| `generalForm` |

### 手順3: `bootstrap/components-list ::` の旧フラグメント名を変更する

手順1の2つ目のパターンにヒットした行に、上記テーブルの **旧フラグメント名** が含まれていれば、
対応する **新フラグメント名** に変更してください（ファイルパスはそのまま）。

例:
- `bootstrap/components-list :: tdUpdateButton(` → `bootstrap/components-list :: updateButton(`
- `bootstrap/components-list :: thText(` → `bootstrap/components-list :: headerText(`
- `bootstrap/components-list :: scriptForTdSubmitButtonWithIds(` → `bootstrap/components-list :: scriptForSubmitButtonWithIds(`
- `bootstrap/components-list :: tdInputText(` → `bootstrap/components-list :: text(`
- `bootstrap/components-list :: internalTdInputCommon(` → `bootstrap/components-list :: internalCommon(`
- `bootstrap/components-list :: internalInsideTdCommon(` → `bootstrap/components-list :: internalInsideCommon(`
- `bootstrap/components-search :: searchText(` → `bootstrap/components-search :: text(`
- `bootstrap/components-search :: searchSelect(` → `bootstrap/components-search :: select(`
- `bootstrap/components-search :: internalSearchInputCommonComponent(` → `bootstrap/components-search :: internalInputCommonComponent(`
- `bootstrap/components-input :: inputText(` → `bootstrap/components-input :: text(`
- `bootstrap/components-input :: inputSelect(` → `bootstrap/components-input :: select(`

### 手順4: 変更例

**変更前:**
```html
<div th:replace="~{bootstrap/components :: inputText('fieldName', '')}"></div>
<div th:replace="~{bootstrap/components-input :: inputText('fieldName', '')}"></div>
<div th:replace="~{bootstrap/components :: submitButton('action', 'label.key', '')}"></div>
<div th:replace="~{bootstrap/components :: thText('fieldName', '')}"></div>
<div th:replace="~{bootstrap/components :: thUpdateButton('')}"></div>
<div th:replace="~{bootstrap/components :: tdUpdateButton('')}"></div>
<div th:replace="~{bootstrap/components :: scriptForSortPagingAndRecordsInScreen('')}"></div>
<div th:replace="~{bootstrap/components :: searchButton('')}"></div>
<div th:replace="~{bootstrap/components :: searchText('fieldName', '')}"></div>
<div th:replace="~{bootstrap/components :: searchSelect('fieldName', '')}"></div>
<div th:replace="~{bootstrap/components :: insertOrUpdateButton(${isInsert}, '')}"></div>
<div th:replace="~{bootstrap/components :: backButton('')}"></div>
<div th:replace="~{bootstrap/components :: generalForm(~{::formContent}, ~{}, ~{}, '', false, '', '')}"></div>
<div th:replace="~{bootstrap/components-list :: tdUpdateButton('')}"></div>
```

**変更後:**
```html
<div th:replace="~{bootstrap/components-input :: text('fieldName', '')}"></div>
<div th:replace="~{bootstrap/components-input :: submitButton('action', 'label.key', '')}"></div>
<div th:replace="~{bootstrap/components-list :: headerText('fieldName', '')}"></div>
<div th:replace="~{bootstrap/components-list :: headerUpdateButton('')}"></div>
<div th:replace="~{bootstrap/components-list :: updateButton('')}"></div>
<div th:replace="~{bootstrap/components-list :: scriptForSortPagingAndRecordsInScreen('')}"></div>
<div th:replace="~{bootstrap/components-search :: searchButton('')}"></div>
<div th:replace="~{bootstrap/components-search :: text('fieldName', '')}"></div>
<div th:replace="~{bootstrap/components-search :: select('fieldName', '')}"></div>
<div th:replace="~{bootstrap/components-edit :: insertOrUpdateButton(${isInsert}, '')}"></div>
<div th:replace="~{bootstrap/components-edit :: backButton('')}"></div>
<div th:replace="~{bootstrap/components-general-form :: generalForm(~{::formContent}, ~{}, ~{}, '', false, '', '')}"></div>
<div th:replace="~{bootstrap/components-list :: updateButton('')}"></div>
```

### 手順5: 変更対象外のフラグメント

`bootstrap/components ::` の参照であっても、上記一覧にないフラグメント名は
**変更不要**です（`bootstrap/components.html` に残っています）。

例（変更不要）:
```html
<div th:replace="~{bootstrap/components :: navbarRoot(...)}"></div>
```

### 手順6: ビルドして確認する

変更後、ビルドが通ることを確認してください。
