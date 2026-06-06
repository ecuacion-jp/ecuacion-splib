# ecuacion-splib-core

## What is it?

`ecuacion-splib-core` is a `spring boot` based core libraries.

## Dependent Ecuacion Libraries

### Automatically Loaded Libraries

- `jp.ecuacion.lib:ecuacion-lib-core`

### Manual Load Needed Libraries

(none)

## Dependent External Libraries

### Automatically Loaded Libraries

- `org.springframework.boot:spring-boot-starter-validation`

(modules depending on `ecuacion-lib-core`)
- `org.apache.commons:commons-lang3`
- `jakarta.mail:jakarta.mail-api`

### Manual Load Needed Libraries

(none)

## Documentation

- [javadoc](https://docs.ecuacion.jp/javadoc/ecuacion-splib-core/)

## Installation

Check [Installation](https://github.com/ecuacion-jp/ecuacion-splib) part of `README` in `ecuacion-splib`.  
The description of dependent `ecuacion` modules is as follows.

```xml
<dependency>
    <groupId>jp.ecuacion.splib</groupId>
    <artifactId>ecuacion-splib-core</artifactId>
    <!-- No version tag needed since ecuacion-splib-parent has dependencyManagement versions. -->
</dependency>
```
