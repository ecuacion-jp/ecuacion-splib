# ecuacion-splib-batch

## What is it?

`ecuacion-splib-batch` is a `spring boot` based batch related libraries.

**What it provides:**

- Standard Job/Step builders — pre-wired `JobBuilder` / `TaskletStepBuilder` via `SplibAppParentBatchConfig`
- Execution logging — job/step start/end logging to a dedicated logger
- Common exception handling — a shared `ExceptionHandler` across tasklets
- Current execution context tracking — AspectJ-based tracking of the currently running job/step/tasklet

See [ecuacion-references-splib](https://references.ecuacion.jp/ecuacion-references-splib/public/showMarkdown/page?id=batch/overview) for details.

## Dependent Ecuacion Libraries

### Automatically Loaded Libraries

- `jp.ecuacion.splib:ecuacion-splib-core`

(modules depending on `ecuacion-splib-core`)
- `jp.ecuacion.lib:ecuacion-lib-core`

### Manual Load Needed Libraries

(none)

## Dependent External Libraries

### Automatically Loaded Libraries

- `org.springframework.boot:spring-boot-starter-batch`
- `org.springframework.boot:spring-boot-starter-aop`

(modules depending on `ecuacion-splib-core`)
- `org.springframework.boot:spring-boot-starter-validation`

(modules depending on `ecuacion-lib-core`)
- `org.apache.commons:commons-lang3`

### Manual Load Needed Libraries

- (If you use `SplibMailUtil`, add `org.springframework.boot:spring-boot-starter-mail`.)


## Documentation

- [ecuacion-references-splib](https://references.ecuacion.jp/ecuacion-references-splib/public/showMarkdown/page?id=batch/overview) — Official reference documentation
- [javadoc](https://docs.ecuacion.jp/javadoc/ecuacion-splib-batch/)

## Installation

Check [Installation](https://github.com/ecuacion-jp/ecuacion-splib) part of `README` in `ecuacion-splib`.  
The description of dependent `ecuacion` modules is as follows.

```xml
<dependency>
    <groupId>jp.ecuacion.splib</groupId>
    <artifactId>ecuacion-splib-batch</artifactId>
    <!-- No version tag needed since ecuacion-splib-parent has dependencyManagement versions. -->
</dependency>
```
