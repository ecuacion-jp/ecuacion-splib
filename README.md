# ecuacion-splib

[![Java CI](https://github.com/ecuacion-jp/ecuacion-splib/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/ecuacion-jp/ecuacion-splib/actions/workflows/ci.yml)
[![GitHub Release](https://img.shields.io/github/v/release/ecuacion-jp/ecuacion-splib)](https://github.com/ecuacion-jp/ecuacion-splib/releases)
[![Maven Central](https://img.shields.io/maven-central/v/jp.ecuacion.splib/ecuacion-splib-core.svg)](https://search.maven.org/artifact/jp.ecuacion.splib/ecuacion-splib-core)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://www.oracle.com/java/technologies/downloads/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

## What is it?

`ecuacion-splib` is a set of `Spring Boot` based libraries that reduce boilerplate
and accelerate development of business applications.

**What's included:**

- `ecuacion-splib-core` — Common foundation (BL, exception handling, common beans / records)
- `ecuacion-splib-ui` — UI display logic shared across `ecuacion-splib-web` / `ecuacion-splib-cli`
- `ecuacion-splib-web` — Web MVC (controllers, services, forms, Thymeleaf)
- `ecuacion-splib-web-jpa` — Web + JPA integration (Edit / SearchList operations)
- `ecuacion-splib-web-markdown` — Markdown-based page rendering
- `ecuacion-splib-jpa` — JPA / Hibernate (Soft Delete, group filter, repositories)
- `ecuacion-splib-batch` — Spring Batch (job and step listeners)
- `ecuacion-splib-cli` — Lightweight foundation for one-shot CUI apps (no Spring Batch)
- `ecuacion-splib-rest` — REST API (exception handling, Spring Security configuration)

`ecuacion-splib` uses `Spring Boot 4`. `ecuacion-lib` and other ecuacion libraries,
utilities and apps use `Jakarta EE 11` for compatibility.

## Versioning

This project follows the spirit of [Semantic Versioning](https://semver.org/). Major version increments indicate breaking changes.

## System Requirements

- JDK 21 or above.

## Documentation

- [ecuacion-references-splib](https://references.ecuacion.jp/ecuacion-references-splib/public/showMarkdown/page?id=home) — Official reference documentation
- javadoc - See the `Documentation` section of the `README` in each module

## Installation

1. Import `ecuacion-splib-parent` as a BOM in your `pom.xml`. This also brings in Spring Boot's
   own dependency management, so you don't need to declare a Spring Boot version separately.
   (Other setup patterns — e.g. using `ecuacion-splib-parent` as the parent POM — are described in
   the `Setup` section of [ecuacion-references-splib](https://references.ecuacion.jp/ecuacion-references-splib/public/showMarkdown/page?id=home).)

    ```xml
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>jp.ecuacion.splib</groupId>
                <artifactId>ecuacion-splib-parent</artifactId>
                <!-- Put the latest release version -->
                <version>x.x.x</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
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

Contributions are welcome! See [CONTRIBUTING.md](CONTRIBUTING.md) for how to report bugs, suggest features, and submit pull requests.
