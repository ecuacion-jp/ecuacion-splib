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
module jp.ecuacion.splib.core {
  exports jp.ecuacion.splib.core.bean;
  exports jp.ecuacion.splib.core.bl;
  exports jp.ecuacion.splib.core.config;
  exports jp.ecuacion.splib.core.constant;
  exports jp.ecuacion.splib.core.container;
  exports jp.ecuacion.splib.core.exceptionhandler;
  exports jp.ecuacion.splib.core.record;
  
  opens jp.ecuacion.splib.core.config to spring.core;

  requires jakarta.annotation;
  requires spring.beans;
  requires transitive spring.context;
  requires spring.core;
  requires transitive jp.ecuacion.lib.core;
  requires org.apache.commons.lang3;
}
