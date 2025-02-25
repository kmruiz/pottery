# pottery
[![Test pottery](https://github.com/kmruiz/pottery/actions/workflows/test.yml/badge.svg)](https://github.com/kmruiz/pottery/actions/workflows/test.yml)
![Requires JDK 17](https://img.shields.io/badge/JDK-17-informational)
[![Release](https://img.shields.io/badge/Release-0.3.2-success)](https://github.com/kmruiz/pottery/releases/tag/0.3.2)
![Downloads](https://img.shields.io/github/downloads/kmruiz/pottery/total)

A simple build system for Java.

## TL;DR

How does a basic pottery definition look like? Well, this repository is already built and released using pottery! You can
take a look at a real, working pottery definition [opening the pottery.yaml file](./pottery.yaml). However, if you are busy, this is
it:

```yml
artifact:
  dependencies: []
  group: group
  id: id
  manifest: {main-class: Main}
  platform:
    produces: [fatjar]
    version: '21'
  version: 1.0.0
parameters: {}
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

## How to Start

**Pottery requires at least Java 21 to run. You can download the OpenJDK 21 from [Adoptium](https://adoptium.net/).**

Download the latest version of the pottery wrapper with curl and add execution rights.

```shell
curl -s -L https://github.com/kmruiz/pottery/releases/latest/download/pottery.sh > pottery.sh && chmod +x pottery.sh
```

And initialise a project for the JDK 21.

```shell
./pottery.sh init example-project group id 1.0.0 -j 21 -p fatjar
cp pottery.sh example-project/
cd example-project
```

Now you can package and execute the fat jar.
```shell
./pottery.sh package
java -jar target/id-1.0.0-fat.jar
```

You can read the documentation in our [web page](https://kmruiz.github.io/pottery/docs/home/).