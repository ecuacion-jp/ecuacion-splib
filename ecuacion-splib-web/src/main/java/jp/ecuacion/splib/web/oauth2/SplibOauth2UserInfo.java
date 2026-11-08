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
package jp.ecuacion.splib.web.oauth2;

import java.io.Serializable;

/**
 * Contains the information of the user received from the OAuth2 provider.
 *
 * <p>Passed to {@link SplibOauth2UserHandler#findOrCreateUser} and
 *     {@link SplibOauth2UserHandlerWithRegistrationConfirm#findUser}.</p>
 *
 * <p>Identify the user with {@code provider} and {@code subject}, not with {@code email}.
 *     An email address may be passed to another person
 *     (for example, an address of a company domain after the employee leaves),
 *     and the new owner would log in as the previous one if the user is found by the email.
 *     {@code subject} differs between providers even for the same person,
 *     so whether the accounts of different providers with the same email are linked
 *     is up to the app.</p>
 *
 * <p>{@code email} is always verified by the provider:
 *     splib makes the login fail when {@code email_verified} is not {@code true}.</p>
 *
 * @param email email address provided by the OAuth2 provider
 * @param name display name provided by the provider
 *     (falls back to the local part of the email address when the provider doesn't provide it)
 * @param provider provider identifier: {@code "GOOGLE"} or {@code "APPLE"}
 * @param subject subject identifier ({@code sub}) of the user, unique and never reassigned
 *     within the provider
 */
public record SplibOauth2UserInfo(String email, String name, String provider, String subject)
    implements Serializable {}
