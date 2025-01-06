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

/**
 * Provides spring boot based web classes.
 */
module jp.ecuacion.splib.web {
  exports jp.ecuacion.splib.web.advice;
  exports jp.ecuacion.splib.web.bean;
  exports jp.ecuacion.splib.web.config;
  exports jp.ecuacion.splib.web.controller;
  exports jp.ecuacion.splib.web.exception;
  exports jp.ecuacion.splib.web.exceptionhandler;
  exports jp.ecuacion.splib.web.form;
  exports jp.ecuacion.splib.web.form.enums;
  exports jp.ecuacion.splib.web.form.record;
  exports jp.ecuacion.splib.web.service;
  exports jp.ecuacion.splib.web.util;

  requires transitive spring.beans;
  requires transitive spring.boot;
  requires transitive spring.boot.autoconfigure;
  requires transitive spring.context;
  requires spring.core;
  requires transitive spring.data.commons;
  requires transitive spring.security.core;
  requires spring.security.crypto;
  requires transitive spring.security.web;
  requires transitive spring.security.config;
  requires transitive spring.web;
  requires transitive spring.webmvc;

  requires transitive jp.ecuacion.splib.core;

  requires transitive jp.ecuacion.lib.core;
  
  requires org.apache.commons.lang3;
  requires thymeleaf;
  requires jakarta.annotation;
  requires transitive org.apache.tomcat.embed.core;

}
