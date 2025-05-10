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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.util.LogUtil;
import jp.ecuacion.lib.core.util.PropertyFileUtil;
import jp.ecuacion.splib.web.constant.SplibWebConstants;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

/**
 * Shows the error page and the next page moving from it.
 */
@Controller
@RequestMapping("/public/error")
public class SplibErrorController implements ErrorController {

  private DetailLogger detailLogger = new DetailLogger(this);

  /**
   * Catches the error by redirecting /public/error and returns the error page.
   * 
   * @param request request
   * @param response response
   * @param model model
   * @return ModelAndView
   */
  @RequestMapping(produces = MediaType.TEXT_HTML_VALUE,
      method = {RequestMethod.POST, RequestMethod.GET})
  public ModelAndView errorHtml(HttpServletRequest request, HttpServletResponse response,
      Model model) {

    // requestに設定されているmodelを追加登録（画面描画のためのparameterもあるので）
    model.addAllAttributes(
        ((Model) request.getAttribute(SplibWebConstants.KEY_MODEL)).asMap());

    final Integer statusCode = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
    final Exception exception = (Exception) request.getAttribute("jakarta.servlet.error.exception");

    // 404の場合
    if (statusCode == 404) {
      detailLogger.info("http status = 404. "
          + "you can find the accessed url from the logs above. (Only when the DEBUG log of "
          + "'org.springframework.security.web.FilterChainProxy' is out)");
      return new ModelAndView("error/404", model.asMap(), HttpStatus.valueOf(statusCode));
    }

    // 404などを含めexceptionがない場合でも本methodは呼ばれてしまい、その場合はexceptionは存在しないので、存在する場合のみ例外をログ出力
    if (exception != null) {
      LogUtil.logSystemError(new DetailLogger(this), exception);

    } else {
      detailLogger.info("SplibErrorController#errorHtml called. httpStatus = " + statusCode);
    }

    return new ModelAndView("error", model.asMap(), HttpStatus.valueOf(statusCode));
  }

  /**
   * Shows the next screen specified in properties file 
   * with key {@code jp.ecuacion.splib.web.system-error.go-to-path}
   * when the button on system error screen is pressed.
   * 
   * @return html file name
   */
  @GetMapping("action")
  public String action() {
    return "redirect:" + PropertyFileUtil.getApp("jp.ecuacion.splib.web.system-error.go-to-path");
  }
}
