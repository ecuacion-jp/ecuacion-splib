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
package jp.ecuacion.splib.jpa.datachangehistory;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.sql.DataSource;
import jp.ecuacion.splib.core.audit.AuditLogContext;
import jp.ecuacion.splib.jpa.datachangehistory.testentity.HistorySample;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * Tests for {@link DataChangeHistoryConfig}, {@link DataChangeRevision} and
 * {@link DataChangeRevisionListener}, with Hibernate on an in-memory H2 database.
 */
@DisplayName("DataChangeHistoryConfig")
class DataChangeHistoryConfigTest {

  /** Like an application's config: scans its own entities. */
  @Configuration
  @EntityScan(basePackageClasses = HistorySample.class)
  static class AppConfig {
  }

  private final ApplicationContextRunner runner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
          HibernateJpaAutoConfiguration.class))
      .withUserConfiguration(AppConfig.class, DataChangeHistoryConfig.class)
      // A database of its own per context, created and dropped with it.
      .withPropertyValues("spring.datasource.generate-unique-name=true");

  /** Without JPA: whether Envers is on the classpath only decides what is registered. */
  private final ApplicationContextRunner withoutEnvers = new ApplicationContextRunner()
      .withUserConfiguration(DataChangeHistoryConfig.class)
      .withClassLoader(new FilteredClassLoader("org.hibernate.envers"));

  @AfterEach
  void tearDown() {
    AuditLogContext.end();
  }

  private static void inTransaction(EntityManagerFactory emf, Consumer<EntityManager> work) {
    EntityManager em = emf.createEntityManager();
    try {
      em.getTransaction().begin();
      work.accept(em);
      em.getTransaction().commit();
    } finally {
      em.close();
    }
  }

  private static boolean tableExists(DataSource dataSource, String name) throws Exception {
    try (Connection con = dataSource.getConnection();
        ResultSet rs = con.getMetaData().getTables(null, null, name.toUpperCase(), null)) {
      return rs.next();
    }
  }

  @Test
  @DisplayName("with Envers: each change is kept with when, who and by which operation")
  void enabled() {
    runner.run(context -> {
      EntityManagerFactory emf = context.getBean(EntityManagerFactory.class);
      AuditLogContext.begin("user1", "admin1");
      String operationId = Objects.requireNonNull(AuditLogContext.current()).getOperationId();

      inTransaction(emf, em -> em.persist(new HistorySample(1L, "before")));
      inTransaction(emf, em -> em.find(HistorySample.class, 1L).setName("after"));

      EntityManager em = emf.createEntityManager();
      List<Number> revisions =
          AuditReaderFactory.get(em).getRevisions(HistorySample.class, 1L);
      assertThat(revisions).hasSize(2);
      assertThat(AuditReaderFactory.get(em)
          .find(HistorySample.class, 1L, revisions.get(0)).getName()).isEqualTo("before");

      List<DataChangeRevision> revs = em.createQuery("from DataChangeRevision",
          DataChangeRevision.class).getResultList();
      assertThat(revs).hasSize(2).allSatisfy(rev -> {
        assertThat(rev.getOperationId()).isEqualTo(operationId);
        assertThat(rev.getActor()).isEqualTo("user1");
        assertThat(rev.getOriginalActor()).isEqualTo("admin1");
        assertThat(rev.getTimestamp()).isNotNull();
      });
      em.close();
    });
  }

  @Test
  @DisplayName("with Envers, outside an operation (e.g. batch): kept, without who and operation")
  void outsideOperation() {
    runner.run(context -> {
      EntityManagerFactory emf = context.getBean(EntityManagerFactory.class);

      inTransaction(emf, em -> em.persist(new HistorySample(1L, "name")));

      EntityManager em = emf.createEntityManager();
      DataChangeRevision rev = em.createQuery("from DataChangeRevision",
          DataChangeRevision.class).getSingleResult();
      assertThat(rev.getActor()).isNull();
      assertThat(rev.getOperationId()).isNull();
      em.close();
    });
  }

  @Test
  @DisplayName("Envers turned off by Hibernate's key: @Audited is ignored, and no history table")
  void turnedOff() {
    runner.withPropertyValues(DataChangeHistoryConfig.PROP_KEY_ENVERS_ENABLED + "=false")
        .run(context -> {
          EntityManagerFactory emf = context.getBean(EntityManagerFactory.class);

          inTransaction(emf, em -> em.persist(new HistorySample(1L, "name")));

          DataSource dataSource = context.getBean(DataSource.class);
          assertThat(tableExists(dataSource, "history_sample")).isTrue();
          assertThat(tableExists(dataSource, "history_sample_aud")).isFalse();
          assertThat(tableExists(dataSource, "revinfo")).isFalse();
        });
  }

  @Test
  @DisplayName("without Envers: starts, with nothing registered")
  void withoutEnvers() {
    withoutEnvers.run(context -> {
      assertThat(context).hasNotFailed();
      assertThat(context).doesNotHaveBean(DataChangeHistoryConfig.class);
    });
  }
}
