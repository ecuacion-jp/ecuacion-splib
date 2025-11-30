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
package jp.ecuacion.splib.web.service;

import jp.ecuacion.splib.core.record.SplibRecord;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Is called from {@link jp.ecuacion.splib.web.advice.SplibDataStoreDependentControllerAdvice}. 
 * Needed methods are defined as abstract methods.
 */
public abstract class SplibDataStoreDependentControllerAdviceService {
  
  /**
   * Obtains logged-in account in {@code SplibRecord} data type from {@code UserDetails} data type.
   * 
   * @param loginUser UserDetails
   * @return logged-in account in {@code SplibRecord} data type
   */
  public abstract SplibRecord getAccGeneral(UserDetails loginUser);

  /**
   * Obtains logged-in admin account 
   * in {@code SplibRecord} data type from {@code UserDetails} data type.
   * 
   * @param loginUser UserDetails
   * @return logged-in admin account in {@code SplibRecord} data type
   */
  public abstract SplibRecord getAccAdmin(UserDetails loginUser);

  /**
   * Obtains group ID from logged-in account in {@code SplibRecord} data type.
   * 
   * @param loginAcc logged-in account in {@code SplibRecord} data type
   * @return group ID
   */
  public abstract Object getGroupId(SplibRecord loginAcc);
}
