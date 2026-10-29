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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jp.ecuacion.splib.core.audit.AuditLogWriter.TargetData;
import org.jspecify.annotations.Nullable;

/**
 * Holds the information {@link AuditLogUtil} collects for one operation, kept per thread.
 *
 * <p>For the modules of splib, not for applications: the static methods are called by the
 *     module that records operations ({@code ecuacion-splib-webmvc}'s
 *     {@code AuditLogInterceptor}), and {@link #putDetail} by modules that add items to
 *     {@code details} ({@code ecuacion-splib-web} for the login state). Applications use
 *     {@link AuditLogUtil} instead.</p>
 */
public final class AuditLogContext {

  private static final ThreadLocal<AuditLogContext> CURRENT = new ThreadLocal<>();

  private final List<TargetData> targets = new ArrayList<>();
  private final Map<String, String> attributes = new LinkedHashMap<>();
  private final Map<String, @Nullable String> details = new LinkedHashMap<>();

  private AuditLogContext() {}

  /** Starts collecting information on the current thread, discarding any left over. */
  public static void begin() {
    CURRENT.set(new AuditLogContext());
  }

  /**
   * Stops collecting on the current thread and returns what was collected.
   *
   * @return what was collected, {@code null} if not collecting
   */
  public static @Nullable AuditLogContext end() {
    AuditLogContext context = CURRENT.get();
    CURRENT.remove();
    return context;
  }

  /**
   * Returns what is being collected on the current thread.
   *
   * @return what is being collected, {@code null} if not collecting
   */
  public static @Nullable AuditLogContext current() {
    return CURRENT.get();
  }

  /**
   * Adds a piece of data the operation is about.
   *
   * @param target the data
   */
  public void addTarget(TargetData target) {
    targets.add(target);
  }

  /**
   * Adds an application-specific item, overwriting the value of the same {@code key}.
   *
   * @param key the item name
   * @param value the value
   */
  public void putAttribute(String key, String value) {
    attributes.put(key, value);
  }

  /**
   * Adds an item to {@code details} of the record, overwriting the value of the same {@code key}.
   *
   * <p>For items that a module other than the one recording the operation knows, such as
   *     {@code ecuacion-splib-web}'s login state.</p>
   *
   * @param key the item name
   * @param value the value
   */
  public void putDetail(String key, @Nullable String value) {
    details.put(key, value);
  }

  /**
   * Returns a copy of the data added so far.
   *
   * @return the data, in the order added
   */
  public List<TargetData> getTargets() {
    return List.copyOf(targets);
  }

  /**
   * Returns a copy of the items added so far.
   *
   * @return the items, in the order first added
   */
  public Map<String, String> getAttributes() {
    return new LinkedHashMap<>(attributes);
  }

  /**
   * Returns a copy of the items added with {@link #putDetail}.
   *
   * @return the items, in the order first added
   */
  public Map<String, @Nullable String> getDetails() {
    return new LinkedHashMap<>(details);
  }
}
