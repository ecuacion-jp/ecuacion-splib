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

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import jp.ecuacion.splib.core.bean.AuthorizationBean;
import jp.ecuacion.splib.web.config.SplibOauth2WebSecurityConfig;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

@DisplayName("SplibOauth2Config")
class SplibOauth2ConfigTest {

  private final WebApplicationContextRunner runner =
      new WebApplicationContextRunner().withUserConfiguration(AppSecurityConfig.class);

  @Test
  @DisplayName("beans are registered without scanning the oauth2 package")
  void beansRegistered() {
    runner.withUserConfiguration(UserHandlerConfig.class).run(ctx -> {
      assertThat(ctx).hasNotFailed();
      assertThat(ctx).hasSingleBean(SplibOauth2AuthSuccessHandler.class);
      assertThat(ctx).hasSingleBean(SplibOauth2RegistrationConfirmService.class);
      assertThat(ctx).doesNotHaveBean(SplibAppleClientSecretService.class);
    });
  }

  @Test
  @DisplayName("SplibAppleClientSecretService is registered when apple team-id is set")
  void appleClientSecretServiceRegistered() {
    runner.withUserConfiguration(UserHandlerConfig.class)
        .withPropertyValues("ecuacion.oauth2.apple.team-id=team",
            "ecuacion.oauth2.apple.key-id=key", "ecuacion.oauth2.apple.client-id=client",
            "ecuacion.oauth2.apple.private-key=dummy")
        .run(ctx -> {
          assertThat(ctx).hasNotFailed();
          assertThat(ctx).hasSingleBean(SplibAppleClientSecretService.class);
        });
  }

  @Test
  @DisplayName("startup fails when no SplibOauth2UserHandler bean is registered")
  void failsWithoutUserHandler() {
    runner.run(ctx -> assertThat(ctx).hasFailed());
  }

  @Test
  @DisplayName("beans are not duplicated when the app also scans the oauth2 package")
  void notDuplicatedWithScan() {
    runner.withUserConfiguration(UserHandlerConfig.class, ScanConfig.class).run(ctx -> {
      assertThat(ctx).hasNotFailed();
      assertThat(ctx).hasSingleBean(SplibOauth2AuthSuccessHandler.class);
      assertThat(ctx).hasSingleBean(SplibOauth2RegistrationConfirmService.class);
    });
  }

  @Configuration
  @EnableWebSecurity
  static class AppSecurityConfig extends SplibOauth2WebSecurityConfig {

    AppSecurityConfig(SplibOauth2AuthSuccessHandler oauth2SuccessHandler,
        @Nullable SplibAppleClientSecretService appleClientSecretService,
        ClientRegistrationRepository clientRegistrationRepository) {
      super(oauth2SuccessHandler, appleClientSecretService, clientRegistrationRepository);
    }

    @Bean
    static ClientRegistrationRepository clientRegistrationRepository() {
      return new InMemoryClientRegistrationRepository(
          CommonOAuth2Provider.GOOGLE.getBuilder("google").clientId("id").clientSecret("secret")
              .build());
    }

    @Override
    protected String getDefaultSuccessUrl() {
      return "/home";
    }

    @Override
    protected String getLoginNeededPage() {
      return "/public/login/page";
    }

    @Override
    protected String getAccessDeniedPage() {
      return "/public/accessDenied/page";
    }

    @Override
    protected @Nullable List<AuthorizationBean> getRoleInfo() {
      return null;
    }

    @Override
    protected @Nullable List<AuthorizationBean> getAuthorityInfo() {
      return null;
    }
  }

  @Configuration
  static class UserHandlerConfig {

    @Bean
    SplibOauth2UserHandler userHandler() {
      return new SplibOauth2UserHandler() {
        @Override
        public UserDetails findOrCreateUser(String email, String name, String provider) {
          return User.withUsername(email).password("").build();
        }

        @Override
        public void afterLoginSuccess(HttpServletRequest request, UserDetails userDetails) {}
      };
    }
  }

  @Configuration
  @ComponentScan("jp.ecuacion.splib.web.oauth2")
  static class ScanConfig {}
}
