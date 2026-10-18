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
package jp.ecuacion.splib.rest.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

/**
 * Verifies that {@link SplibRestSecurityConfig}'s empty {@code UserDetailsService} steps aside when
 * the application registers its own — e.g. for a login form of ecuacion-splib-web in the same
 * application. Spring Security does not set up {@code UserDetailsService}-based authentication at
 * all when two or more such beans exist, which would silently break that login.
 */
@DisplayName("SplibRestSecurityConfig#userDetailsService")
class SplibRestSecurityConfigUserDetailsServiceTest {

  @Configuration
  @EnableWebSecurity
  static class AppRestSecurityConfig extends SplibRestSecurityConfig {
    AppRestSecurityConfig() {
      super(null);
    }
  }

  @Component
  static class AppLoginUserDetailsService implements UserDetailsService {
    @Override
    public UserDetails loadUserByUsername(String username) {
      return User.withUsername(username).password("{noop}x").roles("ADMIN").build();
    }
  }

  private final WebApplicationContextRunner runner = new WebApplicationContextRunner();

  @Test
  @DisplayName("provides an empty one when the application has none")
  void providesEmptyOneWhenAppHasNone() {
    runner.withUserConfiguration(AppRestSecurityConfig.class).run(ctx -> {
      assertThat(ctx).hasNotFailed();
      assertThat(ctx.getBeanNamesForType(UserDetailsService.class)).hasSize(1);
    });
  }

  @Test
  @DisplayName("does not add a second one when the application registers its own")
  void stepsAsideWhenAppHasOne() {
    runner.withUserConfiguration(AppLoginUserDetailsService.class, AppRestSecurityConfig.class)
        .run(ctx -> {
          assertThat(ctx).hasNotFailed();
          assertThat(ctx.getBeansOfType(UserDetailsService.class)).hasSize(1)
              .allSatisfy((name, bean) -> assertThat(bean)
                  .isInstanceOf(AppLoginUserDetailsService.class));
        });
  }
}
