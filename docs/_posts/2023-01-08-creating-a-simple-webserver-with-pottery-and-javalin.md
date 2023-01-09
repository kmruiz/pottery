---
layout: post
title:  "Creating a simple webserver with Pottery and Javalin"
date:   2023-01-08 10:00:00 +0100
categories: how to
---

[Javalin is a lightweight and fast Java and Kotlin framework](https://javalin.io/) which is having a big adoption rate due
to its simplicity and performance. If you are used to work with other frameworks like [ktor](https://ktor.io/) for Kotlin
and [express.js](https://expressjs.com/) for NodeJS, you are not going to be surprised by its API design.

This post summarises how to create a simple HTTP REST API that prints the current date. We will be bundling and testing the
application using [Pottery](/) and [jUnit 5](https://junit.org/junit5/).

## Initialising a new pottery project

To initialise a new Pottery project, you will need the Pottery wrapper. You can download it by issuing the following command in your shell:

```shell
curl -s -L https://github.com/kmruiz/pottery/releases/latest/download/pottery.sh > pottery.sh && chmod +x pottery.sh
```

This will download the latest pottery wrapper (pottery.sh) and add executable permissions. Now, create a new project by
running the following command.

```shell
./pottery.sh init javalin-app "cat.pottery" "javalin_example" "0.0.1"
cp pottery.sh javalin-app/
```

It will take a few seconds and create a new directory called javalin-app with a sample main class. Open the generated
pottery.yaml file and write the following contents:

```yaml
parameters:
  junit.version: "5.9.1" ## latest junit5 version, for testing
  javalin.version: "5.3.0" ## latest javalin version, the web server

artifact:
  group:    "cat.pottery"
  id:       "javalin_example"
  version:  "0.0.1"

  platform:
    version: "17" ## we will need the JDK 17
    produces:
      - "fatjar" ## we will bundle our app as a fatJar

  manifest:
    main-class: "Main"

  dependencies:
    - production: "io.javalin:javalin:${javalin.version}"
    - test: "org.junit.jupiter:junit-jupiter-api:${junit.version}"
```

Open the main class. It should be located in `src/main/java` and it's a file named `Main.java`. Write it's contents with:

```java
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {
        Javalin.create()
                .get("/time", ctx -> {
                    ctx.result(LocalDateTime.now().toString()).status(HttpStatus.OK);
                }).start(9080);
    }
}
```

Now we can bundle the application and run it. Issue the `package` command to download all dependencies and create a fatJar:

```shell
./pottery.sh package 
```
Pottery will download all required dependencies for running your application and bundle the fatJar. The command is finished when
it shows an output similar to:

```
[INFO]  Built fatJar target/javalin_example-0.0.1-fat.jar in 0.88 seconds.
[INFO]  All packages built in 8.06 seconds.
```

Now you can run your application as a normal Java fatJar:

```shell
java -jar target/javalin_example-0.0.1-fat.jar
```

You can call the endpoint by using cURL:

```shell
curl http://localhost:9080/time
```
