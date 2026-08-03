# ecuacion-splib-rest

## What is it?

`ecuacion-splib-rest` is a `spring boot` based REST related libraries.

It is designed to release created war into `tomcat`.

**What it provides:**

- Endpoint-prefix security convention — every endpoint is placed under one of four security policies based on its URL prefix
- API key authentication — header-based (`X-Api-Key`) authentication with a pluggable lookup and plain/hashed comparison mode
- Common exception handling — `ViolationException`, `ResponseStatusException`, and other uncaught exceptions are each translated into an HTTP response

See [ecuacion-references-splib](https://references.ecuacion.jp/ecuacion-references-splib/public/showMarkdown/page?id=rest/overview) for details.

## Dependent Ecuacion Libraries

### Automatically Loaded Libraries

- `jp.ecuacion.splib:ecuacion-splib-core`

(modules depending on `ecuacion-splib-core`)
- `jp.ecuacion.lib:ecuacion-lib-core`

### Manual Load Needed Libraries

(none)

## Dependent External Libraries

### Automatically Loaded Libraries

- `org.springframework.boot:spring-boot-starter-web-services`
- `org.springframework.boot:spring-boot-starter-security`

(modules depending on `ecuacion-splib-core`)
- `org.springframework.boot:spring-boot-starter-validation`

(modules depending on `ecuacion-lib-core`)
- `org.apache.commons:commons-lang3`
- `jakarta.mail:jakarta.mail-api`

### Manual Load Needed Libraries

(none)


## Documentation

- [ecuacion-references-splib](https://references.ecuacion.jp/ecuacion-references-splib/public/showMarkdown/page?id=rest/overview) — Official reference documentation
- [javadoc](https://docs.ecuacion.jp/javadoc/ecuacion-splib-rest/)

## Installation

Check [Installation](https://github.com/ecuacion-jp/ecuacion-splib) part of `README` in `ecuacion-splib`.  
The description of dependent `ecuacion` modules is as follows.

```xml
<dependency>
    <groupId>jp.ecuacion.splib</groupId>
    <artifactId>ecuacion-splib-rest</artifactId>
    <!-- No version tag needed since ecuacion-splib-parent has dependencyManagement versions. -->
</dependency>
```
