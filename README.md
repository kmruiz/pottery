# pottery

A simple build system for Java.

## tl;dr

How does a basic pottery definition look like?

```yaml
parameters:
  gson.version: "2.9.1"
  jackson.version: "2.13.4"
  jupiter.version: "5.9.1"

artifact:
  group:    "cat.json"
  id:       "serde"
  version:  "0.0.1"

  platform:
    produces:
      - "fatjar"
      - "container"
    version: "18"

  manifest:
    main-class: "Main"

  dependencies:
    - production: "com.google.code.gson:gson:${gson.version}"
    - production: "com.fasterxml.jackson.core:jackson-databind:${jackson.version}"
    - test: "org.junit.jupiter:junit-jupiter:${jupiter.version}"
```

## Why pottery?

Build system complexity is real and spend resources and frustrates teams.  Other full-fledged build systems like [Maven](https://maven.apache.org/) 
and [Gradle](https://gradle.org/) are wonderful, but adapting them to each team conventions is time-consuming.

Not all projects require complex setups and plugins. Lightweight configurations based on conventions are effective for on-boarding new teams
and scaling new projects. The [Spring Initializr](https://start.spring.io/) is a well-designed example on how to handle conventions effectively. It's
relatively simple to use, it's easy to understand by new joiners, and just works for most of the use cases.

Pottery aims to lower the complexity of managing a Java project by building on top of conventions, instead of complex configurations. Pottery design principles are:

* Simplicity over completeness
* Aim for most of the projects, not all of them.
* Performance is one of the main levers of a team Developer Experience.
* Integrate with modern development practices as smooth as possible.
* Easy to use on CI, no complex configurations.