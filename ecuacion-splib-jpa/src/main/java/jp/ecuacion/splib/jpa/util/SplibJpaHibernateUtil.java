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

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.hibernate.engine.spi.SessionImplementor;
import org.springframework.stereotype.Component;

/**
 * Treats Hibernate's utilities.
 */
@Component
public class SplibJpaHibernateUtil {

  @PersistenceContext
  private EntityManager em;

  /**
   * Returns entities obtained from persistence context.
   * 
   * <p>This is supposed to use for debug, not for production apps.</p>
   * 
   * @param classes Filters classes. No filter executed when no classes selected.
   * @return entity list.
   */
  public List<Object> getEntitiesFromPersistenceContext(Class<?>... classes) {
    return getAllEntitiesFromPersistenceContext().stream()
        .filter(obj -> classes.length == 0 ? true : Arrays.asList(classes).contains(obj.getClass()))
        .toList();
  }

  /**
   * Returns entities obtained from persistence context.
   * 
   * <p>This is supposed to use for debug, not for production apps.</p>
   * 
   * @param classes Filters classes. No filter executed when no classes selected.
   *        class names are just like "Book", no ".class" or package needed.
   * @return entity list.
   */
  public List<Object> getEntitiesFromPersistenceContext(String[] classes) {
    return getAllEntitiesFromPersistenceContext().stream()
        .filter(obj -> classes.length == 0 ? true
            : Arrays.asList(classes).contains(obj.getClass().getSimpleName().replace(".class", "")))
        .toList();
  }

  private Collection<Object> getAllEntitiesFromPersistenceContext() {
    SessionImplementor sessionImplementor = em.unwrap(SessionImplementor.class);
    org.hibernate.engine.spi.PersistenceContext pc = sessionImplementor.getPersistenceContext();

    return pc.getEntitiesByKey().values();
  }
}
