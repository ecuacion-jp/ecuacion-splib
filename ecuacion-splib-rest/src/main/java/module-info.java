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
module jp.ecuacion.splib.rest {
  exports jp.ecuacion.splib.rest.advice;
  exports jp.ecuacion.splib.rest.config;
  exports jp.ecuacion.splib.rest.controller;
  exports jp.ecuacion.splib.rest.exception;
  
  requires spring.beans;
  requires spring.context;
  requires spring.core;
  requires transitive spring.web;
  requires spring.webmvc;

  requires jp.ecuacion.lib.core;
  requires jp.ecuacion.splib.core;
  requires spring.security.web;
  requires spring.security.config;
  requires spring.boot.autoconfigure;
  requires spring.security.core;
  requires org.apache.tomcat.embed.core;
}
