# ecuacion-splib-web-rest

## What is it?

`ecuacion-splib-rest` is a `spring boot` based REST related libraries.

It is designed to release created war into `tomcat`.

## System Requirements

- JDK 21 or above.

## Dependent Ecuacion Libraries

### Automatically Loaded Libraries

- `jp.ecuacion.lib:ecuacion-splib-core`

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

### Manual Load Needed Libraries

(modules depending on `ecuacion-lib-core`)
- `jakarta.mail:jakarta.mail-api` (If you want to use the mail related utility: `jp.ecuacion.lib.core.util.MailUtil`. `org.springframework.boot:spring-boot-starter-mail` is also fine.)


## Documentation

- [javadoc](https://javadoc.ecuacion.jp/apidocs/ecuacion-splib-rest/)

## Introduction

Check [Introduction](https://github.com/ecuacion-jp/ecuacion-splib) part of `README` in `ecuacion-splib`.  
The description of dependent `ecuacion` modules is as follows.

```xml
<dependency>
    <groupId>jp.ecuacion.splib</groupId>
    <artifactId>ecuacion-splib-rest</artifactId>
    <!-- No version tag needed since ecuacion-splib-parent has dependencyManagement versions. -->
</dependency>
```
