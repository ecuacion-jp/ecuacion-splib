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
module jp.ecuacion.splib.web.jpa {
  exports jp.ecuacion.splib.web.jpa.advice;
  exports jp.ecuacion.splib.web.jpa.util;
  exports jp.ecuacion.splib.web.jpa.exceptionhandler;
  exports jp.ecuacion.splib.web.jpa.service;
  exports jp.ecuacion.splib.web.jpa.controller;

  requires jakarta.annotation;
  requires transitive jakarta.persistence;
  requires jp.ecuacion.lib.core;
  requires jp.ecuacion.lib.jpa;
  requires transitive jp.ecuacion.splib.core;
  requires jp.ecuacion.splib.jpa;
  requires transitive jp.ecuacion.splib.web;
  requires org.apache.commons.lang3;
  requires org.apache.tomcat.embed.core;
  requires spring.beans;
  requires transitive spring.context;
  requires spring.core;
  requires spring.data.commons;
  requires spring.data.jpa;
  requires transitive spring.orm;
  requires transitive spring.security.core;
  requires transitive spring.tx;
  requires spring.web;
  requires spring.webmvc;
}
