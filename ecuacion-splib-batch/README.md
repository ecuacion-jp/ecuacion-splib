# ecuacion-splib-batch

## What is it?

`ecuacion-splib-batch` is a `spring boot` based batch related libraries.

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

- `org.springframework.boot:spring-boot-starter-batch`
- `org.springframework.boot:spring-boot-starter-aop`

(modules depending on `ecuacion-splib-core`)
- `org.springframework.boot:spring-boot-starter-validation`

(modules depending on `ecuacion-lib-core`)
- `org.apache.commons:commons-lang3`
- `org.apache.commons:commons-exec` (@Deprecated. It will be removed in the future release)

### Manual Load Needed Libraries

(modules depending on `ecuacion-lib-core`)
- `jakarta.mail:jakarta.mail-api` (If you want to use the mail related utility: `jp.ecuacion.lib.core.util.MailUtil`. `org.springframework.boot:spring-boot-starter-mail` is also fine.)


## Documentation

- [javadoc](https://javadoc.ecuacion.jp/apidocs/ecuacion-splib-batch/)

## Introduction

Check [Introduction](https://github.com/ecuacion-jp/ecuacion-splib) part of `README` in `ecuacion-splib`.  
The description of dependent `ecuacion` modules is as follows.

```xml
<dependency>
    <groupId>jp.ecuacion.splib</groupId>
    <artifactId>ecuacion-splib-batch</artifactId>
    <!-- No version tag needed since ecuacion-splib-parent has dependencyManagement versions. -->
</dependency>
```
