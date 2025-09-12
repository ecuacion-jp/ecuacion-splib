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
import jp.ecuacion.splib.web.constant.SplibWebConstants;
import jp.ecuacion.splib.web.exception.RedirectToHomePageException;
import jp.ecuacion.splib.web.util.SplibUtil;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Shows the page designated by {@code id} parameter of the url.
 * 
 * <p>To ensure the authentication and security, needed validations are done. 
 *     To secure the page which only logged-in users can see, this feature shows the page 
 *     with {@code data-show-page-login-state} tag 
 *     like [@code data-show-page-login-state="public"}
 *     and shows the page with specified loginState (public / account / admin) </p>
 * 
 * <p>Basically this feature is assumed to use with GET access. <br>
 *     But in some cases(*) this feature is used with POST access, 
 *     so the method is also open to {@code POST}.
 * 
 * <p>(*) One of the cases is that 
 *     in session timeout state login button or other post access button is pressed.<br>
 *     In that case the accessed page was blocked 
 *     because of the session timeout and request is redirected the url with this feature,
 *     and if the original access is POST, 
 *     the redirected access is also POST therefore the post access comes to this feature.
 * </p>
 */
@Controller
@Scope("prototype")
@RequestMapping(value = {"/public/show", "/public/adminShow", "/account/show", "/admin/show",
    "/ecuacion/public/show"})
public class ShowPageController extends SplibBaseController {

  @Autowired
  private SplibUtil util;

  /**
   * Shows the page designated by {@code id} parameter of the url.
   * 
   * @param model model
   * @param page page
   * @return page
   * @throws IOException IOException
   */
  @RequestMapping(value = "page", method = {RequestMethod.POST, RequestMethod.GET})
  public String page(Model model, @RequestParam("id") String page) throws IOException {

    // Validate input string to prevent from attacks.
    String expression = "^[a-zA-Z0-9_\\-/]*$";
    if (!page.matches(expression)) {
      // Redirected to default page because system error is stressful on development...
      throw new RedirectToHomePageException(Level.INFO,
          "jp.ecuacion.splib.web.common.message.htmlFileNameNotAllowed", new String[] {page});
    }

    // pageの存在有無はチェックしておく
    ClassPathResource resource = new ClassPathResource("templates/" + page + ".html");
    if (!resource.exists()) {
      throw new RedirectToHomePageException(Level.INFO,
          "jp.ecuacion.splib.web.common.message.htmlFileNotFound", new String[] {page});
    }

    // htmlタグに"data-show-page-login-state属性があり、その値に現在のloginStateが存在することをチェック
    htmlTagAttributeCheck(page, resource);

    return page;
  }

  /*
   * htmlタグの中に"data-show-page-login-state属性があり、その値に現在のloginStateが存在することをチェックする処理。
   * これだけのためにlibraryを追加しwarを重くするのは無駄なので、文字列操作で対応。
   */
  private void htmlTagAttributeCheck(String page, ClassPathResource resource) throws IOException {
    String html = resource.getContentAsString(Charset.defaultCharset());
    String startTag = "<html";

    // ifの条件の後者は「htmlタグが2つ以上ある場合。
    if (!html.contains(startTag) || html.replace(startTag, "").contains(startTag)) {
      throw new RedirectToHomePageException(Level.INFO,
          "jp.ecuacion.splib.web.common.message.htmlFileNotAllowedToOpen", new String[] {page});
    }

    // htmlタグを抜き出す
    String htmlTag = html.substring(html.indexOf(startTag));
    htmlTag = htmlTag.substring(0, htmlTag.indexOf(">") + 1);

    // 改行・空欄で区切り、"data-show-page-login-state" の属性を抽出
    List<String> list = Arrays.asList(htmlTag.replaceAll("\n", " ").split(" ")).stream()
        .filter(str -> str.contains(SplibWebConstants.KEY_BASE_PAGE_LOGIN_STATE)).toList();

    // チェック
    if (list.size() != 1 || !list.get(0).contains("=")
        || !list.get(0).split("=")[1].contains(util.getLoginState())) {
      throw new RedirectToHomePageException(Level.INFO,
          "jp.ecuacion.splib.web.common.message.htmlFileNotAllowedToOpen", new String[] {page});
    }
  }
}
