# ecuacion-splib

[![Java CI](https://github.com/ecuacion-jp/ecuacion-splib/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/ecuacion-jp/ecuacion-splib/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/ecuacion-jp/ecuacion-splib/branch/main/graph/badge.svg)](https://codecov.io/gh/ecuacion-jp/ecuacion-splib)
[![GitHub Release](https://img.shields.io/github/v/release/ecuacion-jp/ecuacion-splib)](https://github.com/ecuacion-jp/ecuacion-splib/releases)
[![Maven Central](https://img.shields.io/maven-central/v/jp.ecuacion.splib/ecuacion-splib-core.svg)](https://search.maven.org/artifact/jp.ecuacion.splib/ecuacion-splib-core)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://www.oracle.com/java/technologies/downloads/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

## What is it?

`ecuacion-splib` is a set of `Spring Boot` based libraries that reduce boilerplate
and accelerate development of business applications.

**What's included:**

- `ecuacion-splib-core` — Common foundation (BL, exception handling, common beans / records)
- `ecuacion-splib-web` — Web MVC (controllers, services, forms, Thymeleaf)
- `ecuacion-splib-web-jpa` — Web + JPA integration (Edit / SearchList operations)
- `ecuacion-splib-jpa` — JPA / Hibernate (Soft Delete, group filter, repositories)
- `ecuacion-splib-batch` — Spring Batch (job and step listeners)
- `ecuacion-splib-rest` — REST API (exception handling, Spring Security configuration)

`ecuacion-splib` uses `Spring Boot 4`. `ecuacion-lib` and other ecuacion libraries,
utilities and apps use `Jakarta EE 11` for compatibility.

## Versioning

`ecuacion-splib` follows the spirit of Semantic Versioning. Major version increments indicate breaking changes.

## System Requirements

- JDK 21 or above.

## Documentation

- javadoc - See the `Documentation` section of the `README` in each module

## Quick Start

The library provides base classes to extend — you get the boilerplate for free and implement only the application-specific logic.
Full API details are in the javadoc of each module.

### Web Application (`ecuacion-splib-web`)

**1. Security configuration** — Extend `SplibWebSecurityConfig` to configure the built-in login flow, CSRF protection, and route authorization:

```java
@Configuration
@EnableWebSecurity
public class AppSecurityConfig extends SplibWebSecurityConfig {

    public AppSecurityConfig() {
        super(null, null, null); // pass OAuth2 beans here if needed
    }

    @Override protected String getDefaultSuccessUrl() { return "/home/page"; }
    @Override protected String getLoginNeededPage()   { return "/public/login/page"; }
    @Override protected String getAccessDeniedPage()  { return "/public/error/accessDenied"; }

    @Override
    protected List<AuthorizationBean> getRoleInfo() {
        return List.of(new AuthorizationBean("/admin/**", "ADMIN"));
    }

    @Override
    protected List<AuthorizationBean> getAuthorityInfo() { return List.of(); }
}
```

**2. Controller** — Extend `SplibGeneral1FormController` to get GET / POST handling, model setup, and redirect recovery for free:

```java
@Controller
@RequestMapping("/home")
public class HomeController extends SplibGeneral1FormController<HomeForm, HomeService> {
    public HomeController() { super("home"); }
}
```

**3. Service** — Implement `SplibGeneral1FormService` to supply the page data and dropdown selections:

```java
@Service
public class HomeService extends SplibGeneral1FormService<HomeForm> {

    @Override
    public void page(HomeForm form, UserDetails loginUser) throws Exception {
        // load data into form before rendering the page
    }

    @Override
    public void prepareForm(HomeForm form, UserDetails loginUser) {
        // refresh dropdown selections (also called after a validation error)
    }
}
```

### Batch Application (`ecuacion-splib-batch`)

`SplibJobExecutionListener` logs job START / END and tracks the current job name automatically.
Extend it to add application-specific post-processing:

```java
@Component
public class AppJobExecutionListener extends SplibJobExecutionListener {

    @Override
    public void afterJob(JobExecution jobExecution) {
        super.afterJob(jobExecution);
        // application-specific cleanup or notification here
    }
}
```

### REST API (`ecuacion-splib-rest`)

`SplibRestExceptionHandler` is auto-registered via `@RestControllerAdvice` and maps
`HttpStatusException` to the corresponding HTTP status. Throw it anywhere in your REST layer:

```java
// Returns HTTP 404 with an empty body
throw new HttpStatusException(HttpStatus.NOT_FOUND);
```

## Installation

1. Set the following `<parent>` tag to your `pom.xml`.

    ```xml
    <parent>
        <groupId>jp.ecuacion.splib</groupId>
        <artifactId>ecuacion-splib-parent</artifactId>
        <!-- Put the latest release version -->
        <version>x.x.x</version>
    </parent>
    ```

2. Add the required `ecuacion` modules to your `pom.xml`.
   (The following is an example for the `ecuacion-splib-core` module. Check the `Installation` section of the `README` in the module you want to add to your project.)

    ```xml
    <dependency>
        <groupId>jp.ecuacion.splib</groupId>
        <artifactId>ecuacion-splib-core</artifactId>
        <!-- No version tag needed since ecuacion-splib-parent has dependencyManagement versions. -->
    </dependency>
    ```

3. Add the required external modules to your `pom.xml`.
   (Check the `Dependent External Libraries > Manual Load Needed Libraries` section of the `README` in the module you want to add to your project.)

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for bug reports, feature requests, and pull request guidelines.
