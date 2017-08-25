# iroha-scala

Scala library for [Hyperledger Iroha](https://github.com/hyperledger/iroha).


## Install

Install [sbt](http://www.scala-sbt.org/0.13/docs/Setup.html).

```sh
git clone https://github.com/hyperledger/iroha-scala.git
cd iroha-scala
sbt publishLocal
```

## Usage

1.  build.sbt

```sh
libraryDependencies += "org.hyperledger" %% "iroha-scala" % "0.95-SNAPSHOT"
```

## Test

Test required local iroha node with gRPC(50051).

```sh
sbt test
```

## Compile proto
```
sbt compile
```

## License

Copyright 2017 Daisuke SHIMADA.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
