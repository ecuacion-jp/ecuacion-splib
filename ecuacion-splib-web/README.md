# ecuacion-splib-web

## What is it?

`ecuacion-splib-web` is a `spring boot` based web related libraries.

It is basically designed to release created war into `tomcat`, but you can release it to the other application server, 
or you can embed application servers in a war with a little change.

## Quick Start

The library provides base classes to extend — you get the boilerplate for free and implement only the application-specific logic.
Full API details are in the javadoc.

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

## Dependent Ecuacion Libraries

### Automatically Loaded Libraries

- `jp.ecuacion.splib:ecuacion-splib-core`

(modules depending on `ecuacion-splib-core`)
- `jp.ecuacion.lib:ecuacion-lib-core`

### Manual Load Needed Libraries

(none)

## Dependent External Libraries

### Automatically Loaded Libraries

- `org.springframework.boot:spring-boot-starter-web`
- `org.springframework.boot:spring-boot-starter-security`
- `org.springframework.data:spring-data-commons`
- `org.springframework.boot:spring-boot-starter-thymeleaf`
- `nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect`
- `org.thymeleaf.extras:thymeleaf-extras-springsecurity6`

(modules depending on `ecuacion-splib-core`)
- `org.springframework.boot:spring-boot-starter-validation`

(modules depending on `ecuacion-lib-core`)
- `org.apache.commons:commons-lang3`

### Manual Load Needed Libraries

- (If you use `SplibMailUtil`, add `org.springframework.boot:spring-boot-starter-mail`.)


## Documentation

- [javadoc](https://docs.ecuacion.jp/javadoc/ecuacion-splib-web/)

## Installation

Check [Installation](https://github.com/ecuacion-jp/ecuacion-splib) part of `README` in `ecuacion-splib`.  
The description of dependent `ecuacion` modules is as follows.

```xml
<dependency>
    <groupId>jp.ecuacion.splib</groupId>
    <artifactId>ecuacion-splib-web</artifactId>
    <!-- No version tag needed since ecuacion-splib-parent has dependencyManagement versions. -->
</dependency>
```

