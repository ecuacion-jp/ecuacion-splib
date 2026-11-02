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
package jp.ecuacion.splib.jpa.util;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.Map;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SplibJpaFilterUtil")
class SplibJpaFilterUtilTest {

  @SuppressWarnings("null")
  private Session session;

  @BeforeEach
  void setUp() {
    session = mock(Session.class);
    when(session.enableFilter(anyString())).thenAnswer(inv -> mock(Filter.class));
  }

  private SplibJpaFilterUtil create(boolean usesGroupFeature, @Nullable String groupColumnName,
      Map<String, String> customGroupFilterParamNameMap) {
    SplibJpaFilterUtil util = new SplibJpaFilterUtil(true, usesGroupFeature, groupColumnName,
        customGroupFilterParamNameMap) {};
    EntityManager em = mock(EntityManager.class);
    when(em.unwrap(Session.class)).thenReturn(session);
    util.em = em;
    return util;
  }

  @Nested
  @DisplayName("enableGroupFilter()")
  class EnableGroupFilter {

    @Test
    @DisplayName("enables the common group filter and every custom group filter with the group ID")
    void enablesAllGroupFilters() {
      Filter common = mock(Filter.class);
      Filter accGeneral = mock(Filter.class);
      Filter acc = mock(Filter.class);
      when(session.enableFilter("groupFilter")).thenReturn(common);
      when(session.enableFilter("groupFilterAccGeneral")).thenReturn(accGeneral);
      when(session.enableFilter("groupFilterAcc")).thenReturn(acc);

      create(true, "accGeneralId",
          Map.of("groupFilterAccGeneral", "accId", "groupFilterAcc", "id"))
          .enableGroupFilter(1L);

      verify(common).setParameter("accGeneralId", 1L);
      verify(accGeneral).setParameter("accId", 1L);
      verify(acc).setParameter("id", 1L);
    }

    @Test
    @DisplayName("enables only the common group filter when no custom group filter exists")
    void enablesOnlyCommonFilterWithoutCustom() {
      Filter common = mock(Filter.class);
      when(session.enableFilter("groupFilter")).thenReturn(common);

      create(true, "accGroupId", Map.of()).enableGroupFilter(1L);

      verify(common).setParameter("accGroupId", 1L);
      verify(session).enableFilter("groupFilter");
      verify(session, never()).enableFilter("groupFilterAccGroup");
    }

    @Test
    @DisplayName("does nothing when the group feature is not used")
    void doesNothingWithoutGroupFeature() {
      create(false, null, Map.of()).enableGroupFilter(1L);

      verify(session, never()).enableFilter(anyString());
    }
  }

  @Nested
  @DisplayName("disableGroupFilter()")
  class DisableGroupFilter {

    @Test
    @DisplayName("disables the common group filter and every custom group filter")
    void disablesAllGroupFilters() {
      create(true, "accGeneralId",
          Map.of("groupFilterAccGeneral", "accId", "groupFilterAcc", "id"))
          .disableGroupFilter();

      verify(session).disableFilter("groupFilter");
      verify(session).disableFilter("groupFilterAccGeneral");
      verify(session).disableFilter("groupFilterAcc");
    }

    @Test
    @DisplayName("does nothing when the group feature is not used")
    void doesNothingWithoutGroupFeature() {
      create(false, null, Map.of()).disableGroupFilter();

      verify(session, never()).disableFilter(anyString());
    }
  }
}
