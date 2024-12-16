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
module jp.ecuacion.splib.batch {
  exports jp.ecuacion.splib.batch.exceptionhandler;

  requires transitive spring.batch.infrastructure;
  requires spring.context;

  requires jp.ecuacion.splib.core;
  requires jp.ecuacion.lib.core;

  requires jakarta.annotation;
  requires spring.beans;
  requires spring.batch.core;
  requires org.slf4j;
  requires org.aspectj.weaver;
  requires spring.tx;
}
