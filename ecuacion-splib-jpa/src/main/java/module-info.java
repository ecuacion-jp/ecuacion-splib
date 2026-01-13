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
module jp.ecuacion.splib.jpa {
  exports jp.ecuacion.splib.jpa.advice;
  exports jp.ecuacion.splib.jpa.bean;
  exports jp.ecuacion.splib.jpa.config;
  exports jp.ecuacion.splib.jpa.repository;
  exports jp.ecuacion.splib.jpa.util;
	
  requires spring.context;
  requires spring.beans;

  requires transitive jakarta.persistence;

  requires transitive jp.ecuacion.lib.jpa;
  requires transitive jp.ecuacion.splib.core;
  requires transitive spring.orm;
  requires spring.tx;
  requires spring.data.commons;
  requires spring.data.jpa;
  requires jp.ecuacion.lib.core;
  requires org.hibernate.orm.core;
  requires transitive org.aspectj.weaver;
  requires org.apache.commons.lang3;
}
