---
title: Running Tests
permalink: /docs/running-tests/
---

With pottery, you don't need to configure a plugin to run your tests. Just include your
[JUnit 5](https://junit.org/junit5/) API dependency and you are ready to go.

Right now, pottery is defaulting to [JUnit 5](https://junit.org/junit5/) as the default test
runner as it seems widely adopted, easy to run, and efficient. You can use other assertion libraries
on top of that easily if you want to.

To add the JUnit 5 dependency API to your project, open the pottery.yaml and include, in your
dependencies section, a new *test* dependency:

```yaml
- test: "org.junit.jupiter:junit-jupiter-api:${junit.version}"
```

You can find the latest jupiter API version in [MVNRepository](https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api).

Once the dependency has been added, you have to create a test file. Test files follow Maven
conventions:

* They have to be added into the src/test/java directory.
* They should end in Test.java.

So let's create a file `src/test/java/my/project/SampleTest.java` and include the following contents:


```java
package my.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SampleTest {
    @Test
    public void this_test_is_going_to_be_successful() {
        assertTrue(true);
    }

    @Test
    public void this_test_is_going_to_be_failing() {
      assertTrue(false);
    }

    @Test
    @Disabled
    public void this_test_is_going_to_be_skipped() {
      assertTrue(false);
    }
}
```

Running the test suite is done by issuing the test command to pottery:

```shell
./pottery.sh test
```

Pottery will download the necessary dependencies and run the tests, generating an
output like this one:

```shell
[WARN]  CONTAINER_BUILDER is not configured. Packaging a 'container' or 'docker' artifact won't work.
[INFO]  Running pottery version 0.3.2
[ERROR] 1 failed tests.
[ERROR] () this_test_is_going_to_be_failing(): expected: <true> but was: <false>
[WARN]  1 skipped tests.
[INFO]  1 of 2 (50.00%) successful tests.
[INFO]  Tests ran in 0.73 seconds.
```

Pottery will fail if there is a test that fails. Let's remove the failing test
and run pottery.sh again. The test file should look like the following:

```java
package my.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SampleTest {
    @Test
    public void this_test_is_going_to_be_successful() {
        assertTrue(true);
    }
    
    @Test
    @Disabled
    public void this_test_is_going_to_be_skipped() {
      assertTrue(false);
    }
}
```

Running pottery now:

```shell
./pottery.sh test
```

Will show the following output:

```shell
[WARN]  CONTAINER_BUILDER is not configured. Packaging a 'container' or 'docker' artifact won't work.
[INFO]  Running pottery version 0.3.2
[WARN]  1 skipped tests.
[INFO]  1 of 1 (100.00%) successful tests.
[INFO]  Tests ran in 0.72 seconds.
```

And now it worked! However, we have one skipped test, which is usually a smell in your project. By default,
pottery will not fail if there are skipped tests, however, you can run the test command in strict mode, that will
fail if there are any skipped tests. To do so, provide the -x (or --strict) flag to pottery:

```shell
./pottery.sh test -x
```

The output will look like:

```shell
[INFO]  Running pottery version 0.3.2
[INFO]  Nothing to compile.
[ERROR] 1 skipped tests. Failing due to strict mode.
[INFO]  1 of 1 (100.00%) successful tests.
[INFO]  Tests ran in 0.49 seconds.
```

It is recommended to run always the strict mode in your CI pipelines.
