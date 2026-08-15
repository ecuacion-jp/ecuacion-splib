# ecuacion-splib-cli

## What is it?

`ecuacion-splib-cli` is a `spring boot` based CLI (command-line interface) related library.

It is a lightweight foundation for one-shot CUI apps run interactively by the person using
them — Spring (DI, `@PropertySource`, message resolution) is kept, but there's no Spring Batch:
no Job/Step/JobRepository, no metadata database.

**What it provides:**

- One-shot startup — `SplibCliApplication#main` runs the app's single `SplibCliRunner` bean once and exits
- Custom startup banner — replaces Spring Boot's own banner with ecuacion-splib's own brand mark, version, and (optionally) the app's own name/version; toggle with the `jp.ecuacion.splib.cli.banner-mode` property (`on`/`off`, default `on`)
- Silent-by-default logging — no console noise and no log file unless the app configures its own logger/appender; `--verbose` prints a full stack trace on top of the normal concise message
- Timestamped console messages — start/completion/error messages are all prefixed with `[yyyy-MM-dd HH:mm:ss]`
- Running indicator — an animated "..." indicator pinned to the last console line while `execute` runs, automatically disabled when not attached to a real interactive terminal
- Common exception handling — `SplibExceptionHandler` prints a concise localized message (with a bullet list for `ViolationException`), always logs the full detail via `LogUtil#logSystemError`, and optionally invokes a `SplibExceptionHandlerAction` your app supplies

See [ecuacion-references-splib](https://references.ecuacion.jp/ecuacion-references-splib/public/showMarkdown/page?id=cli/overview) for details.

## Dependent Ecuacion Libraries

### Automatically Loaded Libraries

- `jp.ecuacion.splib:ecuacion-splib-core`
- `jp.ecuacion.splib:ecuacion-splib-ui`

(modules depending on `ecuacion-splib-core`)
- `jp.ecuacion.lib:ecuacion-lib-core`

### Manual Load Needed Libraries

(none)

## Dependent External Libraries

### Automatically Loaded Libraries

- `org.springframework.boot:spring-boot-starter`

(modules depending on `ecuacion-splib-core`)
- `org.springframework.boot:spring-boot-starter-validation`

(modules depending on `ecuacion-lib-core`)
- `org.apache.commons:commons-lang3`

### Manual Load Needed Libraries

- (If you use `SplibMailUtil`, add `org.springframework.boot:spring-boot-starter-mail`.)


## Documentation

- [ecuacion-references-splib](https://references.ecuacion.jp/ecuacion-references-splib/public/showMarkdown/page?id=cli/overview) — Official reference documentation
- [javadoc](https://docs.ecuacion.jp/javadoc/ecuacion-splib-cli/)

## Installation

Check [Installation](https://github.com/ecuacion-jp/ecuacion-splib) part of `README` in `ecuacion-splib`.  
The description of dependent `ecuacion` modules is as follows.

```xml
<dependency>
    <groupId>jp.ecuacion.splib</groupId>
    <artifactId>ecuacion-splib-cli</artifactId>
    <!-- No version tag needed since ecuacion-splib-parent has dependencyManagement versions. -->
</dependency>
```
