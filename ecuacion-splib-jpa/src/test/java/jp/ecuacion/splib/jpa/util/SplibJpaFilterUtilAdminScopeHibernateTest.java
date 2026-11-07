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

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import jp.ecuacion.splib.jpa.util.testentity.AdminScopeSample;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/** Tests the admin scope filter with Hibernate on an in-memory H2 database. */
@DisplayName("SplibJpaFilterUtil: admin scope filter with Hibernate")
class SplibJpaFilterUtilAdminScopeHibernateTest {

  @Configuration
  @EntityScan(basePackageClasses = AdminScopeSample.class)
  static class AppConfig {
  }

  private final ApplicationContextRunner runner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
          HibernateJpaAutoConfiguration.class))
      .withUserConfiguration(AppConfig.class)
      .withPropertyValues("spring.datasource.generate-unique-name=true",
          "spring.jpa.properties.hibernate.session_factory.statement_inspector="
              + SqlRecorder.class.getName());

  /** Records the SQL. H2 accepts "in ()", which is a syntax error on PostgreSQL. */
  public static class SqlRecorder implements StatementInspector {
    private static final long serialVersionUID = 1L;
    static final List<String> SQL_LIST = new CopyOnWriteArrayList<>();

    @SuppressWarnings("null")
    @Override
    public String inspect(String sql) {
      SQL_LIST.add(sql);
      return sql;
    }
  }

  private void withEntityManager(Consumer<EntityManager> test) {
    runner.run(context -> {
      try (EntityManager em = context.getBean(EntityManagerFactory.class).createEntityManager()) {
        em.getTransaction().begin();
        em.persist(new AdminScopeSample(1L, 10L));
        em.persist(new AdminScopeSample(2L, 20L));
        em.persist(new AdminScopeSample(3L, 30L));
        em.flush();
        em.clear();
        test.accept(em);
        em.getTransaction().rollback();
      }
    });
  }

  @SuppressWarnings("null")
  private List<Long> findIdsWithScope(EntityManager em, Collection<?> values) {
    SplibJpaFilterUtil util = new SplibJpaFilterUtil(false, false, null, Map.of(),
        Map.of("adminScopeFilter", "targetSystemId")) {};
    util.em = em;
    util.enableAdminScopeFilter(values);

    return em.createQuery("select e.id from AdminScopeSample e order by e.id", Long.class)
        .getResultList();
  }

  @Test
  @DisplayName("returns the records of the values")
  void multipleValues() {
    withEntityManager(em -> assertThat(findIdsWithScope(em, List.of(10L, 30L)))
        .containsExactly(1L, 3L));
  }

  @Test
  @DisplayName("returns no records when the values are empty")
  void emptyValues() {
    withEntityManager(em -> {
      SqlRecorder.SQL_LIST.clear();

      assertThat(findIdsWithScope(em, List.of())).isEmpty();
      assertThat(SqlRecorder.SQL_LIST).singleElement().asString().contains("in (?)");
    });
  }
}
