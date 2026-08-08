# ecuacion-splib-dependencies

## What is it?

`ecuacion-splib-dependencies` is the parent POM used by `ecuacion-splib`'s own modules
(`ecuacion-splib-core`, `ecuacion-splib-web`, etc.), not by application projects.

Its parent is `ecuacion-lib-dependencies` (not `ecuacion-splib-parent`), since Maven only allows a
single `<parent>`. `ecuacion-splib-parent`'s own `dependencyManagement` (the `spring-boot-dependencies`
BOM, the `ecuacion-util-parent` BOM, and ecuacion-splib module versions) is instead pulled in via a
BOM import — the same technique `ecuacion-splib-parent` itself already uses to bring in
`ecuacion-util-parent`.

This gives `ecuacion-splib-dependencies` the "strict" build tooling that `ecuacion-lib-dependencies`
applies to ecuacion-lib's own modules (checkstyle, spotbugs, automatic license-header insertion,
NullAway/Error Prone static analysis, an actual `jspecify` dependency), plus what's specific to
ecuacion-splib's own build:

- Actual (not just managed) dependencies on `spring-boot-devtools` and `spring-boot-starter-test`.
- The full `-parameters` / NullAway-ErrorProne compiler configuration (`ecuacion-splib-parent` only
  keeps `-parameters` / `-Xlint`, since those two also need to apply to general applications; see
  `ecuacion-splib-parent`'s own `pom.xml` for the split rationale).
- `maven-surefire-plugin`'s `useModulePath=false`, needed because ecuacion-splib depends on
  ecuacion-lib's named JPMS modules.
- `maven-war-plugin` (`failOnMissingWebXml=false`, and bundling `NOTICE.txt` / `LICENSE.txt` into
  the WAR's META-INF when present), plus the equivalent unconditional `NOTICE.txt` / `LICENSE.txt`
  copy into the jar's META-INF. Neither is standard Spring Boot behavior, so both are kept out of
  `ecuacion-splib-parent` to avoid surprising general consumers.

`ecuacion-splib-parent` deliberately does **not** inherit any of this, so that general applications
can use it as their own parent POM without picking up ecuacion's internal build enforcement or real
dependencies. See Pattern 3 vs. Pattern 4 in the Setup section of
[ecuacion-references-splib](https://references.ecuacion.jp/ecuacion-references-splib/public/showMarkdown/page?id=home)
for the difference.

## Dependent External Libraries

(none directly; see `ecuacion-lib-dependencies` and `ecuacion-splib-parent`)

## Documentation

(none)

## Installation

You never want to install this directly. It is used as the parent POM of ecuacion-splib's own
modules only.
