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
package jp.ecuacion.splib.jpa.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDate;
import java.time.YearMonth;
import org.jspecify.annotations.Nullable;

/**
 * Converts a {@link YearMonth} entity attribute to and from a {@code date} database column.
 *
 * <p>PostgreSQL (and most RDBMSs) have no dedicated year-month column type, so the value is
 * persisted as the 1st day of the month.</p>
 */
@Converter(autoApply = true)
public class YearMonthConverter implements AttributeConverter<YearMonth, LocalDate> {

  @Override
  public @Nullable LocalDate convertToDatabaseColumn(@Nullable YearMonth attribute) {
    return attribute == null ? null : attribute.atDay(1);
  }

  @Override
  public @Nullable YearMonth convertToEntityAttribute(@Nullable LocalDate dbData) {
    return dbData == null ? null : YearMonth.from(dbData);
  }
}
