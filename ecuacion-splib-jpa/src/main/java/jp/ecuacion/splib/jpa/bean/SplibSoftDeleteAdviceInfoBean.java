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
package jp.ecuacion.splib.jpa.bean;

public class SplibSoftDeleteAdviceInfoBean {
  private static ThreadLocal<Object> thGroupId = new ThreadLocal<>();

  public static Object getGroupId() {
    return thGroupId.get();
  }

  public static void setGroupId(Object groupId) {
    thGroupId.set(groupId);
  }
}
