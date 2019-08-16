# iroha-scala

Scala library for [Hyperledger Iroha](https://github.com/hyperledger/iroha).

## For end users

Add the following library dependency into your project:

```scala
libraryDependencies += "org.hyperledger" %% "iroha-scala" % "1.06-SNAPSHOT"
```

----

## For developers

### Requirements

 * JDK8+ is required
 * integration tests require one or more Iroha nodes
 * a snapshot build of ed25519-sha3-java
 
### Building dependencies

```bash
#!/bin/bash

mkdir ${HOME}/workspace
cd ${HOME}/workspace
git clone https://github.com/frgomes/ed25519-sha3-java
cd ed25519-sha3-java
git checkout RG0001-Code_review
./sbt publishLocal
```

### Building iroha-scala

```bash
#!/bin/bash

mkdir ${HOME}/workspace
cd ${HOME}/workspace
git clone https://github.com/frgomes/iroha-scala
cd iroha-scala
git checkout RG0001-Code_review
./sbt compile
```

### Unit tests

```bash
#!/bin/bash

cd ${HOME}/workspace/iroha-scala
$ ./sbt test
```

### Integration tests

TBD
