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
package jp.ecuacion.splib.web.adminlogin;

/**
 * Contains the state of an admin account which can log in, returned by
 *     {@link SplibAdminAccountProvider}.
 *
 * @param mailAddress the mail address the login code is sent to
 * @param stampSource the values which log out the sessions of the account when changed, joined
 *     into a string, like the mail address, the role and the hash of the password.
 *     Its SHA-256 hash is kept in the session (the value itself is not), and compared on every
 *     request.
 */
public record SplibAdminAccountState(String mailAddress, String stampSource) {}
