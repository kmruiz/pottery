---
title: Your First Project
permalink: /docs/home/
redirect_from: /docs/index.html
---

**Pottery requires at least Java 17 (the latest LTS) to run. You can download the OpenJDK 17 from [Adoptium](https://adoptium.net/).**

Pottery is split in two different artifacts. Usually, you will work only with the pottery wrapper (pottery.sh). The pottery wrapper
will download the necessary pottery version to run your project automatically and will be your entrypoint to issuing pottery
commands.

To download the wrapper, you can just run this command in your preferred shell:

```shell
curl -s -L https://github.com/kmruiz/pottery/releases/latest/download/pottery.sh > pottery.sh && chmod +x pottery.sh
```

You can store the pottery wrapper anywhere. However, as it is a small shell script, it is convenient to store it within the source code.

To create a new project with pottery, use the pottery.sh wrapper init command, that will create a folder for your project, the pottery.yml
definition file and a sample Java source file.

```shell
./pottery.sh init example-project group id 1.0.0 -j 17 -p fatjar
```

* _example-project_ is the folder where the project is going to be created. If it doesn't exist, it will be created.
* _group_ Group Id of the maven artifact.
* _id_ Artifact Id of the maven artifact.
* _1.0.0_ Initial version of the maven artifact.
* _-j 17_ Target JDK version. Defaults to 17.
* _-p fatjar_ Artifact type to be produced. It will produce a fatjar artifact with the necessary dependencies to be run.

Created the project, now we can `cd` into it and package it.

```shell
cp pottery.sh example-project/ # bring the pottery.sh along so it can be committed
cd example-project
```

Package the project and run it.

```shell
./pottery.sh package
java -jar target/id-1.0.0-fat.jar
```

Now you are ready to go!
