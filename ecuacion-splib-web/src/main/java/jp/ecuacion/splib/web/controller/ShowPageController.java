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
package jp.ecuacion.splib.web.controller;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import jp.ecuacion.splib.web.exception.internal.HtmlFileNotAllowedToOpenException;
import jp.ecuacion.splib.web.exception.internal.HtmlFileNotFoundException;
import jp.ecuacion.splib.web.util.SplibUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 処理をせず、htmlを表示するだけのcontrollerが必要な場合に使用する。 そのようなControllerが複数あると邪魔なのでそれらを集約する目的で使用。
 * ひとつのクラスの中に、本クラスの子クラスを複数内部クラスとして定義することで効率的に複数のページ移動を実現。
 * 
 * <p>
 * page=xxxとparameterで指定すれば、xxx.htmlを表示してくれるcontroller。セキュリティも考慮。
 * </p>
 * 
 * <p>
 * 権限で画面表示の制御を行う場合には本Controllerは使用不可かも。 （/public/showPage/page?page=xxx
 * をsecurityConfigに設定すればいけるかもしれないが未確認）
 * </p>
 */
@Controller
@Scope("prototype")
@RequestMapping(
    value = {"/public/showPage", "/public/adminShowPage", "/account/showPage", "/admin/showPage"})
public class ShowPageController extends SplibBaseController {

  @Autowired
  private SplibUtil util;

  /**
   * 表示したいpageをURLのparameterで指定することで表示するCnotroller。
   * 
   * <p>
   * 便利な反面、セキュリティガキになるところなので必要なチェック等は行う。 特に、未login状態からlogin後のpageが見えたりするのは問題なので、htmlタグに
   * data-show-page-login-state="public"
   * のように、この属性名で、かつloginStateが指定されたもの（複数ある場合はカンマ区切りで）にのみ閲覧が可能とする。
   * </p>
   * 
   * <p>
   * 基本はgetでの使用を想定。 Session Timeout状態で、ログインボタンや他のPOST系ボタンを押す場合、session timeoutで弾かれ
   * 本ページにredirectされるが、そのredirectもPOSTで行われるため本処理にPOSTで入ってくることがある。 それを考慮しget/post両方を受け取る設定にしておく。
   * </p>
   */
  @RequestMapping(value = "page", method = {RequestMethod.POST, RequestMethod.GET})
  public String page(Model model, @RequestParam("page") String page) throws IOException {

    // no checkだと脆弱性をつかれる可能性があるので、使用可能文字は限定しておく。
    String expression = "^[a-zA-Z0-9_]*$";
    if (!page.matches(expression)) {
      // システムエラーにすると面倒なので、home的なページへのredirectとしておく。
      // /public/home/pageとしているが、存在しなければredirectされて適切なページに飛ぶはず・・
      return "redirect:/public/home/page";
    }

    // pageの存在有無はチェックしておく
    ClassPathResource resource = new ClassPathResource("templates/" + page + ".html");
    if (!resource.exists()) {
      throw new HtmlFileNotFoundException(page);
    }

    // htmlタグに"data-show-page-login-state属性があり、その値に現在のloginStateが存在することをチェック
    htmlTagAttributeCheck(page, resource);

    return page;
  }

  /**
   * htmlタグの中に"data-show-page-login-state属性があり、その値に現在のloginStateが存在することをチェックす流処理。
   * これだけのためにlibraryを追加しwarを重くするのは無駄なので、文字列操作で対応。
   */
  private void htmlTagAttributeCheck(String page, ClassPathResource resource) throws IOException {
    String html = resource.getContentAsString(Charset.defaultCharset());
    String startTag = "<html";

    // ifの条件の後者は「htmlタグが2つ以上ある場合。
    if (!html.contains(startTag) || html.replace(startTag, "").contains(startTag)) {
      throw new HtmlFileNotFoundException(page);
    }

    // htmlタグを抜き出す
    String htmlTag = html.substring(html.indexOf(startTag));
    htmlTag = htmlTag.substring(0, htmlTag.indexOf(">") + 1);

    // 改行・空欄で区切り、"data-show-page-login-state" の属性を抽出
    List<String> list = Arrays.asList(htmlTag.replaceAll("\n", " ").split(" ")).stream()
        .filter(str -> str.contains("data-show-page-login-state")).toList();

    // チェック
    if (list.size() != 1 || !list.get(0).contains("=")
        || !list.get(0).split("=")[1].contains(util.getLoginState())) {
      throw new HtmlFileNotAllowedToOpenException(page);
    }
  }
}
