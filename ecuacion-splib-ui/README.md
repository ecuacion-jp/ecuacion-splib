# ecuacion-splib-ui

## What is it?

`ecuacion-splib-ui` is a `spring boot` based library holding UI display logic shared across
ecuacion-splib's UI-facing modules (`ecuacion-splib-web`, `ecuacion-splib-cli`, and potentially
more in the future). It holds only framework-agnostic pure logic — nothing web-, console-, or
otherwise UI-technology-specific.

**What it provides:**

- Required-field error masking — `SplibViolationUtil#excludeConstraintViolationsMaskedByRequiredError` filters out a `ConstraintViolation` already implied by a required-field `BusinessViolation` on the same item, so a user isn't shown two errors for the same missing value
- Shared UI constants — `SplibUiConstants` (e.g. the not-empty message key) used by both `ecuacion-splib-web` and `ecuacion-splib-cli`

## Dependent Ecuacion Libraries

### Automatically Loaded Libraries

- `jp.ecuacion.splib:ecuacion-splib-core`

(modules depending on `ecuacion-splib-core`)
- `jp.ecuacion.lib:ecuacion-lib-core`

### Manual Load Needed Libraries

(none)

## Dependent External Libraries

### Automatically Loaded Libraries

(modules depending on `ecuacion-splib-core`)
- `org.springframework.boot:spring-boot-starter-validation`

(modules depending on `ecuacion-lib-core`)
- `org.apache.commons:commons-lang3`

### Manual Load Needed Libraries

- (If you use `SplibMailUtil`, add `org.springframework.boot:spring-boot-starter-mail`.)


## Documentation

- [javadoc](https://docs.ecuacion.jp/javadoc/ecuacion-splib-ui/)

## Installation

Check [Installation](https://github.com/ecuacion-jp/ecuacion-splib) part of `README` in `ecuacion-splib`.  
The description of dependent `ecuacion` modules is as follows.

```xml
<dependency>
    <groupId>jp.ecuacion.splib</groupId>
    <artifactId>ecuacion-splib-ui</artifactId>
    <!-- No version tag needed since ecuacion-splib-parent has dependencyManagement versions. -->
</dependency>
```
