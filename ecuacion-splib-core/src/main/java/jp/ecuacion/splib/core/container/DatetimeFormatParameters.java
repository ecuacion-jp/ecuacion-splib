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
package jp.ecuacion.splib.core.container;

import java.time.ZoneOffset;

/**
 * Provides DatetimeFormatParameters.
 */
public class DatetimeFormatParameters {

  private ZoneOffset zoneOffset;

  private String dateTimeFormat = "yyyy-MM-dd HH:mm:ss";
  private String timestampFormat = "yyyy-MM-dd HH:mm:ss.SSSSSS XXX"; 
  private String dateFormat = "yyyy-MM-dd";
  private String timeFormat = "HH:mm:ss";

  /**
   * Constructs a new instance.
   */
  public DatetimeFormatParameters() {

  }

  /**
   * Constructs a new instance.
   * 
   * @param zoneOffset zoneOffset
   */
  public DatetimeFormatParameters(ZoneOffset zoneOffset) {
    this.zoneOffset = zoneOffset;
  }

  public ZoneOffset getZoneOffset() {
    return zoneOffset;
  }

  public void setZoneOffset(ZoneOffset zoneOffset) {
    this.zoneOffset = zoneOffset;
  }

  /**
   * javascriptのDateオブジェクトのgetTimezoneOffset() メソッドにて取得される値を引数に渡す形でのoffset指定.
   * 
   *  <p>UTC+0900（日本）だと -9(時間) 60(分) = -540 という値。 これを「ZoneOffset.ofHours(9)」の形で保持。</p>
   */
  public void setZoneOffsetWithJsMinutes(int minutes) {
    int hour = -1 * minutes / 60;
    int minute = -1 * minutes % 60;

    zoneOffset = ZoneOffset.ofHoursMinutes(hour, minute);
  }

  public String getDateTimeFormat() {
    return dateTimeFormat;
  }

  public void setDateTimeFormat(String dateTimeFormat) {
    this.dateTimeFormat = dateTimeFormat;
  }

  public String getTimestampFormat() {
    return timestampFormat;
  }

  public void setTimestampFormat(String timestampFormat) {
    this.timestampFormat = timestampFormat;
  }

  public String getDateFormat() {
    return dateFormat;
  }

  public void setDateFormat(String dateFormat) {
    this.dateFormat = dateFormat;
  }

  public String getTimeFormat() {
    return timeFormat;
  }

  public void setTimeFormat(String timeFormat) {
    this.timeFormat = timeFormat;
  }
}
