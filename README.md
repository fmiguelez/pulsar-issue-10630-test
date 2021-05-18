# Test to validate Apache Pulsar Issue [#10630](https://github.com/apache/pulsar/issues/10630)

## Requirements

This has been tested against Pulsar 2.7.1.

Requires Maven and Docker (Docker Desktop will do on Windows or MacOS). 

## Tests

To run the tests execute from command line:

```bash 
$ mvn verify
```

If you want to run the tests from an IDE (e.g. Eclipse or IntelliJ) you need a Pulsar instance up first. You can use `docker-compose.yml` file in `src/test/resources` folder.

```bash 
$ docker-compose -f src/test/resources/docker-compose.yml up -d
```

Until issue is solved following test will fail:

```console 
[INFO] Results:
[INFO]
[ERROR] Failures:
[ERROR]   PartitionedTopicSeekIT.testEarliest:57->testSeek:98 expected: <[A1, B1, C1, D1]> but was: <[A1, A0, B0, B1, C0, C1, D0, D1, A1, B1, C1, D1]>
[INFO]
[ERROR] Tests run: 2, Failures: 1, Errors: 0, Skipped: 0
```