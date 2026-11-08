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
 * Provides the protection of the admin login: the login code sent by mail, the check of the
 *     session of the logged-in admin, and the limit of wrong passwords.
 *
 * <p>See {@link jp.ecuacion.splib.web.adminlogin.SplibAdminLoginProtection}.</p>
 */
@NullMarked
package jp.ecuacion.splib.web.adminlogin;

import org.jspecify.annotations.NullMarked;
