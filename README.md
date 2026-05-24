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

(See `Documentation` part of the `README` in each module)

## Installation

1. Add the following `<repositories>` tag to your `pom.xml` (as a child of the `<project>` tag).

    ```xml
    <repositories>
        <repository>
            <id>ecuacion-repo-http</id>
            <name>ecuacion-repo-http</name>
            <url>http://maven-repo.ecuacion.jp/public</url>
        </repository>
    </repositories>
    ```

2. Set the following `<parent>` tag to your `pom.xml`.

    ```xml
    <parent>
        <groupId>jp.ecuacion.splib</groupId>
        <artifactId>ecuacion-splib-parent</artifactId>
        <!-- Put the latest release version -->
        <version>x.x.x</version>
    </parent>
    ```

3. Add the required `ecuacion` modules to your `pom.xml`.
   (The following is an example for the `ecuacion-splib-core` module. Check the `Installation` section of the `README` in the module you want to add to your project.)

    ```xml
    <dependency>
        <groupId>jp.ecuacion.splib</groupId>
        <artifactId>ecuacion-splib-core</artifactId>
        <!-- No version tag needed since ecuacion-splib-parent has dependencyManagement versions. -->
    </dependency>
    ```

4. Add the required external modules to your `pom.xml`.
   (Check the `Dependent External Libraries > Manual Load Needed Libraries` section of the `README` in the module you want to add to your project.)

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for bug reports, feature requests, and pull request guidelines.
