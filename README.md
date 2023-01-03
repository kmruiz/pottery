# pottery
[![Test pottery](https://github.com/kmruiz/pottery/actions/workflows/test.yml/badge.svg)](https://github.com/kmruiz/pottery/actions/workflows/test.yml)
![JDK 17](https://img.shields.io/badge/JDK-17-informational)
[![Release](https://img.shields.io/badge/Release-0.1.0-success)](https://github.com/kmruiz/pottery/releases/tag/0.1.0)
![Downloads](https://img.shields.io/github/downloads/kmruiz/pottery/total)

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

Build system complexity is real and spends resources and frustrates teams.  Other full-fledged build systems like [Maven](https://maven.apache.org/) 
and [Gradle](https://gradle.org/) are wonderful, but adapting them to each team conventions is time-consuming.

Not all projects require complex setups and plugins. Lightweight configurations based on conventions are effective for on-boarding new teams
and scaling new projects. The [Spring Initializr](https://start.spring.io/) is a well-designed example on how to handle conventions effectively. It's
relatively simple to use, it's easy to understand by new joiners, and just works for most of the use cases.

Pottery aims to lower the complexity of managing a Java project by building on top of conventions, instead of complex configurations. Pottery design principles are:

* Simplicity over completeness
* Aim for most of the projects, not all of them.
* Performance is one of the main levers of a team's Developer Experience.
* Integrate with modern development practices as smooth as possible.
* Easy to use on CI, no complex configurations.

## How to start

Download the latest version of the pottery wrapper with curl and add execution rights.

```shell
$> curl -s -L https://github.com/kmruiz/pottery/releases/latest/download/pottery.sh > pottery.sh && chmod +x pottery.sh
```

And initialise a project
```shell

```