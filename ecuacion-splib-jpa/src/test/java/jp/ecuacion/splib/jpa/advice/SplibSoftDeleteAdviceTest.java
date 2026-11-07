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
package jp.ecuacion.splib.jpa.advice;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import jp.ecuacion.splib.jpa.bean.SplibControllerAdviceInfoBean;
import jp.ecuacion.splib.jpa.entity.SplibEntity;
import jp.ecuacion.splib.jpa.repository.SplibRepository;
import jp.ecuacion.splib.jpa.util.SplibJpaFilterUtil;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

@DisplayName("SplibSoftDeleteAdvice")
class SplibSoftDeleteAdviceTest {

  @SuppressWarnings("null")
  private SplibJpaFilterUtil filterUtil;
  @SuppressWarnings("null")
  private SplibRepository<SplibEntity, Object> repo;
  @SuppressWarnings("null")
  private JoinPoint joinPoint;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setUp() {
    filterUtil = mock(SplibJpaFilterUtil.class);
    repo = mock(SplibRepository.class);

    SplibEntity entity = mock(SplibEntity.class);
    when(entity.hasSoftDeleteField()).thenReturn(true);

    joinPoint = mock(JoinPoint.class);
    when(joinPoint.getArgs()).thenReturn(new Object[] {entity});
    when(joinPoint.getThis()).thenReturn(repo);

    SplibControllerAdviceInfoBean.setGroupId(1L);
  }

  @AfterEach
  void tearDown() {
    SplibControllerAdviceInfoBean.removeGroupId();
  }

  private SplibSoftDeleteAdvice createAdvice() {
    return new SplibSoftDeleteAdvice(filterUtil) {};
  }

  @SuppressWarnings("null")
  @Test
  @DisplayName("disables the filters during the procedure and restores them with the group ID")
  void restoresFilters() {
    when(repo.findByIdAndSoftDeleteFieldTrueFromAllGroups(any())).thenReturn(Optional.empty());

    createAdvice().onBeforeSave(joinPoint);

    InOrder order = inOrder(filterUtil, repo);
    order.verify(filterUtil).disableAllFilters();
    order.verify(repo).findByIdAndSoftDeleteFieldTrueFromAllGroups(any());
    order.verify(filterUtil).enableAllFilters(1L);
  }

  @SuppressWarnings("null")
  @Test
  @DisplayName("restores the filters with the group ID when the procedure throws an exception")
  void restoresFiltersOnException() {
    when(repo.findByIdAndSoftDeleteFieldTrueFromAllGroups(any()))
        .thenThrow(new IllegalStateException("test"));

    assertThatThrownBy(() -> createAdvice().onBeforeSave(joinPoint))
        .isInstanceOf(IllegalStateException.class);

    InOrder order = inOrder(filterUtil, repo);
    order.verify(filterUtil).disableAllFilters();
    order.verify(repo).findByIdAndSoftDeleteFieldTrueFromAllGroups(any());
    order.verify(filterUtil).enableAllFilters(1L);
  }
}
