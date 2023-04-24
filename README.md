# Test to validate Apache Pulsar Issue [#10630](https://github.com/apache/pulsar/issues/10630)

## Requirements

This has been tested against Pulsar 2.8.4.

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
[INFO]
[INFO] Results:
[INFO]
[ERROR] Failures:
[ERROR]   PartitionedTopicSeekIT.testSeekTimestampFromEarliest:59->testSeek:123 size ==> expected: <4> but was: <9>
[ERROR] Errors:
[ERROR]   PartitionedTopicSeekIT.testSeekMessageIdFromLatest:69->testSeek:111 » PulsarClient
[INFO]
[ERROR] Tests run: 3, Failures: 1, Errors: 1, Skipped: 0
```
