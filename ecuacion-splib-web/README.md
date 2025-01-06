## system template

### warning

### ShowPageController

- /public/show/page?id="xxx"でxxx.htmlを表示可能
- セキュリティの観点から、htmlタグにdata-show-page-login-state="public"が指定されたhtmlファイルのみ、そのloginStateにて閲覧可能。

### show config

- /ecuacion/public/config/page で、用意されたページが表示される

### show error

- error画面の表示とそこからログイン画面など次の画面に行く処理
- defaultの404（
### default 404.html


### PCのzoneOffsetを取得

		<div th:replace="~{bootstrap/components :: sendZoneOffset('')}"></div>
		htmlで以下を呼ぶだけで、sessionの"zoneOffset" attribute に格納される。
```html
<div th:replace="~{bootstrap/components :: sendZoneOffset('')}"></div>
```