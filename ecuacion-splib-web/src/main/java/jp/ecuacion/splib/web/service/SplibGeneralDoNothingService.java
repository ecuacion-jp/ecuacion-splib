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

import org.springframework.stereotype.Service;

/**
 * Provides a general service with no method.
 * 
 * <p>It is useful when general page is needed but no service is needed.
 *     It happens when a form exists in a page but no need to process the result of the input form.
 *     For instance, login page is often the case because login procedure is not implemented 
 *     by the controller, because the spring security does it instead.</p>
 */
@Service
public class SplibGeneralDoNothingService extends SplibGeneralService {
}
