# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Java Coding Rules

### Style Standards
- Follows **Google Java Style Guide** (enforced by Checkstyle in CI)
- Indentation: **2 spaces** (no tabs)
- Max line length: **100 characters** (excluding package/import statements) — **applies to comments too**
- Encoding: **UTF-8**

### Imports
- Wildcard imports (`.*`) are **prohibited**
- Imports are sorted automatically (follow IDE auto-organize imports)

### Javadoc
- **All public classes, methods, and fields must have Javadoc**
- `@return` and `@param` tags must not be omitted
- When editing existing files, review and update Javadoc for any modified methods

### License Header
- All Java files must have the Apache 2.0 license header at the top
- Follow the same format as existing files

## File Creation and Editing Rules

- Always refer to existing files in the same package before creating a new one
- When adding to a package that has `package-info.java`, check its contents first

## Build and Test

```bash
# Build and install locally
mvn clean install

# Build skipping tests
mvn clean install -DskipTests

# Code style check (Google Checkstyle-based)
mvn checkstyle:check

# Static analysis (SpotBugs)
mvn spotbugs:check

# Build a specific module only
mvn clean install -pl ecuacion-splib-core
```

There are almost no test classes in this project; testing is designed to be done by the applications that use this library.

## Architecture Overview

A **shared library suite** for Spring Boot 3 / Jakarta EE 10 / JDK 21+, designed to accelerate development and reduce boilerplate code.

### Module Structure and Dependencies

```
ecuacion-lib-core (external library)
  └─ ecuacion-splib-core         # Foundation: BL, exception handling, common beans
       ├─ ecuacion-splib-web      # Web MVC: controllers, services, forms, Thymeleaf
       │    └─ ecuacion-splib-web-jpa  # Web + JPA integration (Edit/SearchList operations)
       ├─ ecuacion-splib-jpa      # JPA/Hibernate: Soft Delete, group filter, repositories
       ├─ ecuacion-splib-batch    # Spring Batch: job and step listeners
       └─ ecuacion-splib-rest     # REST API: exception handling, Spring Security config
```

### Key Design Patterns

**Template Method Pattern**
`SplibGeneralController<S>` provides a standard flow for CRUD operations, which applications extend and use. The service layer similarly extends `SplibGeneralService` (`Splib1FormService`, `Splib2FormsService`, etc.).

**Business Logic Layer (BL)**
- `SplibCoreBl` — Common logic such as duplicate checks and child record existence checks
- `SplibJpaBl<E, I, V>` — JPA-specific logic such as optimistic lock control

**Hibernate Filter Mechanism**
The JPA module uses Hibernate session filters to transparently implement soft delete (`softDeleteFilter`) and group isolation (`groupFilter`). Activation and deactivation are managed by `SplibJpaFilterUtil`.

**AOP (Controller Advice)**
Each module has a `@ControllerAdvice` / `@RestControllerAdvice` for global exception handling, layered as `SplibControllerAdvice` (web), `SplibJpaControllerAdvice` (web-jpa), and `SplibRestControllerAdvice` (rest).

**Forms and Records**
The web layer combines form classes (`SplibGeneralForm`, `SplibEditForm`, `SplibSearchForm`) with Java Records for data transfer. `PagerInfo` handles pagination.

### Security

Uses Spring Security 6. Provides `SplibWebSecurityConfig` (for general users) and `SplibWebSecurityConfigForAdmin` (for administrators), with support for admin user-switch functionality.

### Resource Locations

- `messages_splib_web.properties` / `messages_splib_web_ja.properties` — Error and UI messages
- `item_names_splib_web.properties` — Form field names
- `templates/bootstrap/` — Thymeleaf template fragments

## Verification (Always run after editing Java files and fix all violations before finishing)

```bash
mvn checkstyle:check spotbugs:check
mvn javadoc:javadoc
```

The most common violations are:
- Checkstyle: Line length over 100 characters (including comments and Javadoc)
- Checkstyle: Missing Javadoc on public members
- Checkstyle: Wildcard imports
- SpotBugs: Using reflection to access private fields (use `protected` scope workarounds where needed)
