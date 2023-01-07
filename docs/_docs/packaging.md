---
title: Packaging
permalink: /docs/packaging/
---

Pottery comes with four default packaging mechanisms that do not require
additional configuration. Packaging is the action of generating a bundle that
can be distributed for customers. The four packaging mechanisms are:

* **library**: Generates a JAR file without dependencies.
* **fatjar**: Generates a fatJar with dependencies that can be executed.
* **container**: Generates a Container (e.g Docker container) with the running application. You can specify `docker` instead of container, they are synonyms.
* **native**: Uses the GraalVM native-image AoT to generate a native executable.

To configure the distribution mechanism of your pottery project, you need to describe
the `artifact.platform.produces` properties. Take this pottery.yaml snippet as an example:

```yaml
artifact:
  group:    "cat.pottery"
  id:       "pottery"
  version:  "0.3.2"

  platform:
    version: "17"
    produces: "fatjar"
```

This `pottery.yaml` configuration specifies that we will deliver pottery as a fatjar that can be run. This means that
pottery requires a JDK to be run, and that JDK needs to be at least the JDK 17 (the latest LTS).

Any `pottery.yaml` project can define multiple artifact outputs by defining `produces` as an array. Let's say we want
pottery to be distributed both as a `fatjar` and as a `container`. We can specify so as:

```yaml
artifact:
  group:    "cat.pottery"
  id:       "pottery"
  version:  "0.3.2"

  platform:
    version: "17"
    produces: 
      - "fatjar"
      - "container" 
```

Running the `package` command:

```shell
./pottery.sh package
````

will generate the following output:

```yaml
[INFO]  Running pottery version 0.3.2
[INFO]  Built fatJar target/pottery-0.3.2-fat.jar in 0.24 seconds.
[INFO]  Built container cat.pottery/pottery:0.3.2 in 1.82 seconds.
[INFO]  All packages built in 2.99 seconds.
```
## library Artifact

Generating a library artifact is simple, as it only requires a JDK, that you already
have to run Pottery itself. To build a library artifact, specify the `library` output in the
`produces` section. For example:

```yaml
artifact:
  group:    "cat.pottery"
  id:       "pottery"
  version:  "0.3.2"

  platform:
    version: "17"
    produces: "library"
```

## fatJar Artifact

Generating an executable fatJar is like building a library, as it only requires a JDK, that you already
have to run Pottery itself. Fat Jars specify a `main class`, which is the class to be executed when the fat jar is run
by the JDK. To build a fatJar artifact, specify the `fatjar` output in the `produces` section and a `main-class` specification
in the manifest section. For example:

```yaml
artifact:
  group:    "cat.pottery"
  id:       "pottery"
  version:  "0.3.2"

  platform:
    version: "17"
    produces: "fatjar"
  manifest:
    main-class: "cat.pottery.ui.cli.Bootstrap"
```

## container/docker Artifact

To generate a `container` or `docker` artifact, you need a container builder. A container builder is the
executable that you use to build a container from a Containerfile. We don't support (yet) all container builders,
but you can see the ones we support in the following table:

| Builder | Support | Comments                                        |
|---------|---------|-------------------------------------------------|
| podman  | Yes     | Default development environment. Battle tested. | 
| docker  | Yes     | Tested basic scenarios. Will be maintained.     |

To build a container, specify the `container` or `docker` output artifact in the `produces` section:

```yaml
artifact:
  group:    "cat.pottery"
  id:       "pottery"
  version:  "0.3.2"

  platform:
    version: "17"
    produces: "container"
  manifest:
    main-class: "cat.pottery.ui.cli.Bootstrap"
```

Now, run the pottery command with the CONTAINER_BUILDER environment variable,
pointing to the executable that will build the container.

```shell
CONTAINER_BUILDER=podman src/main/sh/pottery.sh package
```

The output command will be as:

```shell
[INFO]  Running pottery version 0.3.0
[INFO]  Built fatJar target/pottery-0.3.2-fat.jar in 0.24 seconds.
[INFO]  Built container cat.pottery/pottery:0.3.2 in 1.82 seconds.
[INFO]  All packages built in 2.99 seconds.
```

Containers are bundled with the specified OpenJDK version and a fatJar wit the application. At this point,
we don't support a native image in a container, [but we are likely to implement it in the future](https://github.com/kmruiz/pottery/issues/2).

## native Artifact

Building a native artifact requires the [GraalVM Native Image](https://www.graalvm.org/22.0/reference-manual/native-image/). Please follow the instructions
in the [GraalVM documentation](https://www.graalvm.org/22.0/reference-manual/native-image/) on how to install it in your Operating System. This documentation
assumes that GraalVM Native Image is already installed.

Once GraalVM Native Image is installed, you can configure your pottery.yaml to generate a native artifact:

```yaml
artifact:
  group:    "cat.pottery"
  id:       "pottery"
  version:  "0.3.2"

  platform:
    version: "17"
    produces: "native"
  manifest:
    main-class: "cat.pottery.ui.cli.Bootstrap"
```

To provide to Pottery which GraalVM version to use, you need to provide the GRAALVM_HOME
environment variable, pointing to where GraalVM is installed. Then, run pottery:

```shell
GRAALVM_HOME=/usr/share/graalvm/graalvm-ce-java17-22.2.0 ./pottery.sh package
```

The output will be as:

```shell
[INFO]  Running pottery version 0.3.0
[INFO]  Generating required fatJar for native image.
[INFO]  Built fatJar target/pottery-0.3.2-fat.jar in 0.25 seconds.
[INFO]  Built GraalVM native image target/pottery-0.3.2 in 27.01 seconds.
[INFO]  All packages built in 27.88 seconds.

```
