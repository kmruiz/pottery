---
# Feel free to add content and custom Front Matter to this file.
# To modify the layout, see https://jekyllrb.com/docs/themes/#overriding-theme-defaults

layout: home
---

<h1>
  <img style="display: block; width: 50%; margin: auto;" src="/pottery/assets/img/logomain.svg" alt="Pottery"/>
</h1>

## What is Pottery

Pottery is a build system for Java designed to be simple, fast and easy to use.

* **Easy to use**: Just a YAML file, no XMLs, no DSLs, no complex configuration.
* **Fast**: Parallel downloads and incremental compilation by default.
* **Lightweight**: The pottery fat-jar is less than 2MB, the pottery wrapper is 1KB.
* **Ready to use**: Can compile to a library, a fatJar, a container or a native image by default.

## Getting Started

You can find the documentation [here](/pottery/docs/home/).

## An example pottery file:
```yaml
parameters:
  junit.version: "5.9.1"
  snakeyaml.version: "1.33"
  chalk.version: "1.0.2"
  picocli.version: "4.7.0"
  junit.platform.version: "1.9.0"
  junit.engine.version: "5.9.1"

artifact:
  group:    "cat.pottery"
  id:       "pottery"
  version:  "0.3.2"

  platform:
    version: "17"
    produces: "fatjar"

  manifest:
    main-class: "cat.pottery.ui.cli.Bootstrap"

  dependencies:
    - production: "org.yaml:snakeyaml:${snakeyaml.version}"
    - production: "com.github.tomas-langer:chalk:${chalk.version}"
    - production: "info.picocli:picocli:${picocli.version}"
    - production: "org.junit.platform:junit-platform-launcher:${junit.platform.version}"
    - production: "org.junit.jupiter:junit-jupiter-engine:${junit.engine.version}"
    - test: "org.junit.jupiter:junit-jupiter-api:${junit.version}"
```
