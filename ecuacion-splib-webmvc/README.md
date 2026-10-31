# ecuacion-splib-webmvc

## What is it?

`ecuacion-splib-webmvc` is a `spring boot` based library holding Spring MVC (servlet) logic
shared by `ecuacion-splib-web` and `ecuacion-splib-rest`. Both depend on it, so applications
do not need to declare it themselves.

Logic that does not depend on the servlet API or Spring MVC belongs in `ecuacion-splib-core`
instead, so that it can also be used from batch, CLI, and other future UI technologies.

**What it provides:**

- Audit log — records operations (requests to controller methods), logins, login failures, logouts and switch users, enabled by `jp.ecuacion.splib.core.audit-log.enabled=true`. The record type and where it is written (`jp.ecuacion.splib.core.audit`) are in `ecuacion-splib-core`.

See [ecuacion-references-splib](https://references.ecuacion.jp/ecuacion-references-splib/public/showMarkdown/page?id=core/audit/audit-log) for details.

## Dependent Ecuacion Libraries

### Automatically Loaded Libraries

- `jp.ecuacion.splib:ecuacion-splib-core`

(modules depending on `ecuacion-splib-core`)
- `jp.ecuacion.lib:ecuacion-lib-core`

### Manual Load Needed Libraries

(none)

## Dependent External Libraries

### Automatically Loaded Libraries

- `org.springframework.boot:spring-boot-starter-webmvc`
- `org.springframework.boot:spring-boot-starter-security`

(modules depending on `ecuacion-splib-core`)
- `org.springframework.boot:spring-boot-starter-validation`

(modules depending on `ecuacion-lib-core`)
- `org.apache.commons:commons-lang3`

### Manual Load Needed Libraries

(none)


## Documentation

- Official reference documentation - [ecuacion-references-splib](https://references.ecuacion.jp/ecuacion-references-splib/public/showMarkdown/page?id=home)
- javadoc - [javadoc.io](https://javadoc.io/doc/jp.ecuacion.splib/ecuacion-splib-webmvc/latest/index.html)

## Installation

Not needed: it is loaded automatically through `ecuacion-splib-web` / `ecuacion-splib-rest`.
