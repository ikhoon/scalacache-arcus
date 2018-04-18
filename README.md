# ScalaCache for Naver Arcus

## 
```scala
libraryDependencies += "com.github.ikhoon" %% "scalacache-arcus" % "lastest.version"

```


## 왜 만들었나?
[ScalaCache](https://github.com/cb372/scalacache)는 redis, guava, caffeine, memcached cache api를 동일한 interface에서 사용할수 있게 제공해주는 미들웨어이다.

ScalaCache에는 memcached client인 [spymemcached module](https://github.com/cb372/scalacache/tree/master/modules/memcached)이 있지만 이를 [arcus java client](https://github.com/naver/arcus-java-client)와 같이 사용할수 없다.
[Arcus java client](https://github.com/naver/arcus-java-client)는 과거의 spymemcached 기준이기 때문에 최신 spymemcached와 binary compatibility가 깨어지기 때문이다.

Arcus를 [scalacache](https://github.com/cb372/scalacache)의 다양한 API를 쓸수 있게 arcus용 scala cahce module을 별도로 생성하였다.

## 앞으로 할일

* ~sonartype repo에 등록하기~
* ~travis에 arcus unit test설정~
* ~example code 넣기~
* ~scala cross version build~
* arcus factory with typesafe config

