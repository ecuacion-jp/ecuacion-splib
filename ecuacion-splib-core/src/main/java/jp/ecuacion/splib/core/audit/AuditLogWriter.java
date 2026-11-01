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
package jp.ecuacion.splib.core.audit;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.splib.core.audit.enums.AuditLogEventType;
import jp.ecuacion.splib.core.audit.enums.AuditLogResult;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes audit log records to the {@value #LOGGER_NAME} logger at INFO level, one JSON object per
 * line.
 *
 * <p>Including {@code logback-spring-audit-log.xml} (in {@code ecuacion-splib-core}) in the
 *     application's {@code logback-spring.xml} routes this logger to
 *     {@code ${log-dir}/${project-name}-audit.log} only. Old files are expected to be removed with
 *     {@code ecuacion-tool-housekeep-files}, like the other log files.</p>
 *
 * <p>Like the other logs of ecuacion-splib, records are written only through logback; to keep
 *     them elsewhere (e.g. a log server), forward the file.</p>
 *
 * <p>Every value is JSON-escaped, so a value that comes from the request (the URI, a username
 *     typed in) cannot break the line into forged records.</p>
 */
public class AuditLogWriter {

  /** The name of the logger the records are written to. */
  public static final String LOGGER_NAME = "audit-logger";

  private final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

  /**
   * Constructs a new instance.
   *
   * <p>Logs a warning when {@value #LOGGER_NAME} is not enabled at INFO level, since the records
   *     would then be silently dropped — typically {@code logback-spring-audit-log.xml} is not
   *     included and the root logger's level is WARN or coarser.</p>
   */
  public AuditLogWriter() {
    if (!logger.isInfoEnabled()) {
      new DetailLogger(this).warn("The audit log is enabled, but the logger \"" + LOGGER_NAME
          + "\" is not enabled at INFO level, so no audit log record is written. Include "
          + "logback-spring-audit-log.xml in logback-spring.xml.");
    }
  }

  /**
   * Writes {@code record}.
   *
   * @param record the record
   */
  public void write(Entry record) {
    logger.info(toJson(record));
  }

  /**
   * Converts {@code record} to a single-line JSON object.
   */
  static String toJson(Entry record) {
    StringBuilder sb = new StringBuilder("{");
    field(sb, "timestamp", record.timestamp().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    field(sb, "operationId", record.operationId());
    field(sb, "eventType", record.eventType().name());
    field(sb, "result", record.result().name());
    field(sb, "actor", record.actor());
    field(sb, "originalActor", record.originalActor());
    // Written at the top level, so that a record of a Spring MVC application reads like
    // {"timestamp":...,"actor":...,"uri":...,...}.
    for (Map.Entry<String, @Nullable String> entry : record.details().entrySet()) {
      field(sb, entry.getKey(), entry.getValue());
    }

    sb.append(",\"targets\":[");
    boolean first = true;
    for (TargetData target : record.targets()) {
      sb.append(first ? "{" : ",{");
      first = false;
      field(sb, "type", target.type());
      field(sb, "id", target.id());
      sb.append("}");
    }
    sb.append("]");

    field(sb, "error", record.error());

    sb.append(",\"attributes\":{");
    first = true;
    for (Map.Entry<String, String> entry : record.attributes().entrySet()) {
      if (!first) {
        sb.append(",");
      }
      first = false;
      appendString(sb, entry.getKey());
      sb.append(":");
      appendString(sb, entry.getValue());
    }
    sb.append("}}");

    return sb.toString();
  }

  /** Appends {@code "name":value}, preceded by a comma unless it is the first member. */
  private static void field(StringBuilder sb, String name, @Nullable String value) {
    char last = sb.charAt(sb.length() - 1);
    if (last != '{') {
      sb.append(",");
    }
    appendString(sb, name);
    sb.append(":");
    if (value == null) {
      sb.append("null");
    } else {
      appendString(sb, value);
    }
  }

  /** Appends {@code value} as a JSON string literal (RFC 8259). */
  private static void appendString(StringBuilder sb, String value) {
    sb.append('"');
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '"' -> sb.append("\\\"");
        case '\\' -> sb.append("\\\\");
        case '\n' -> sb.append("\\n");
        case '\r' -> sb.append("\\r");
        case '\t' -> sb.append("\\t");
        default -> {
          if (c < 0x20 || c == 0x7f) {
            sb.append(String.format("\\u%04x", (int) c));
          } else {
            sb.append(c);
          }
        }
      }
    }
    sb.append('"');
  }

  /**
   * Holds one audit log record, passed to {@link #write}.
   *
   * <p>Holds the items common to every kind of application. Items specific to how the application
   *     runs — for a Spring MVC application, the request's URI, HTTP method, client address and so
   *     on — are in {@code details}, filled by the module that records the events
   *     ({@code ecuacion-splib-webmvc} for Spring MVC). So the set of items can differ between
   *     kinds of applications, but is the same for every record of one application.</p>
   *
   * <p>Never holds request parameters, and holds only the exception class name and message IDs —
   *     not the rendered message — of an error, so that secrets or personal data the user typed in
   *     do not end up in the audit log. The one value typed in by the user that is recorded as is,
   *     is the username of a failed login ({@code actor}), 
   *     needed to detect brute-force attacks.</p>
   *
   * @param timestamp when the event happened
   * @param operationId the ID of the operation ({@link AuditLogContext#getOperationId()}), which
   *     the data change history made by the operation also holds; {@code null} for events that
   *     are not operations, such as logins
   * @param eventType the kind of the event
   * @param result the result of the event
   * @param actor who did it: {@code Authentication#getName()} of the logged-in user (the key ID for
   *     an {@code X-Api-Key} client), or the username as typed for a failed login; {@code null} if
   *     not logged in
   * @param originalActor during a switch user, the administrator who switched to {@code actor};
   *     {@code null} otherwise
   * @param details items specific to how the application runs, in the order written (see above);
   *     a value may be {@code null}
   * @param targets the data the operation was about, added by {@link AuditLogUtil#addTarget}
   * @param error the exception class name, followed by message IDs for a
   *     {@code ViolationException}; {@code null} if no exception occurred
   * @param attributes items the application added with {@link AuditLogUtil#putAttribute}
   */
  public record Entry(OffsetDateTime timestamp, @Nullable String operationId,
      AuditLogEventType eventType, AuditLogResult result,
      @Nullable String actor, @Nullable String originalActor, Map<String, @Nullable String> details,
      List<TargetData> targets, @Nullable String error, Map<String, String> attributes) {
  }

  /**
   * Holds one piece of data an operation was about.
   *
   * @param type the kind of the data, such as the entity name
   * @param id the identifier of the data, such as the primary key
   */
  public record TargetData(String type, String id) {
  }
}
