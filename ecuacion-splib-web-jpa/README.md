# ecuacion-splib-web-jpa

## What is it?

`ecuacion-splib-web-jpa` is a `spring boot` based web and JPA related libraries.

It is designed to release created war into `tomcat`.

## System Requirements

- JDK 21 or above.

## Dependent Ecuacion Libraries

### Automatically Loaded Libraries

- `jp.ecuacion.lib:ecuacion-splib-web`
- `jp.ecuacion.lib:ecuacion-splib-jpa`

(modules depending on `ecuacion-splib-web`)
- `jp.ecuacion.lib:ecuacion-splib-core`

(modules depending on `ecuacion-splib-jpa`)
- `jp.ecuacion.lib:ecuacion-lib-jpa`

(modules depending on `ecuacion-splib-core`)
- `jp.ecuacion.lib:ecuacion-lib-core`

### Manual Load Needed Libraries

(none)

## Dependent External Libraries

### Automatically Loaded Libraries

(modules depending on `ecuacion-splib-web`)
- `org.springframework.boot:spring-boot-starter-web`
- `org.springframework.boot:spring-boot-starter-security`
- `org.springframework.data:spring-data-commons`
- `org.springframework.boot:spring-boot-starter-thymeleaf`
- `nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect`
- `org.thymeleaf.extras:thymeleaf-extras-springsecurity6`

(modules depending on `ecuacion-splib-jpa`)
- `org.springframework.boot:spring-boot-starter-data-jpa`

(modules depending on `ecuacion-splib-core`)
- `org.springframework.boot:spring-boot-starter-validation`

(modules depending on `ecuacion-lib-core`)
- `org.apache.commons:commons-lang3`
- `org.apache.commons:commons-exec` (@Deprecated. It will be removed in the future release)

### Manual Load Needed Libraries

(modules depending on `ecuacion-lib-core`)
- `jakarta.mail:jakarta.mail-api` (If you want to use the mail related utility: `jp.ecuacion.lib.core.util.MailUtil`. `org.springframework.boot:spring-boot-starter-mail` is also fine.)


## Documentation

- [javadoc](https://javadoc.ecuacion.jp/apidocs/ecuacion-splib-web-jpa/)

## introduction

Check [Introduction](https://github.com/ecuacion-jp/ecuacion-splib) part of `README` in `ecuacion-splib`.  
The description of dependent `ecuacion` modules is as follows.

```xml
<dependency>
    <groupId>jp.ecuacion.splib</groupId>
    <artifactId>ecuacion-splib-web-jpa</artifactId>
    <!-- No version tag needed since ecuacion-splib-parent has dependencyManagement versions. -->
</dependency>
```
