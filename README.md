# DSL Parser

A simple parser library for expressions and domain specific languages, written in Kotlin.

## Use

### Maven dependency

```xml
<repositories>
    <repository>
        <id>dsl-parser-mvn-repo</id>
        <url>https://github.com/spissvinkel/dsl-parser-kt/raw/mvn-repo</url>
    </repository>
</repositories>
```
```xml
<dependencies>
    <dependency>
        <groupId>no.simenstorsveen</groupId>
        <artifactId>dsl-parser</artifactId>
        <version>0.2.2</version>
    </dependency>
</dependencies>
```

## Develop

### Install dependencies

Java JDK 21+ and Maven 3.8+ are required to build the project

### Build project

```bash
$ mvn clean package
```

## References

This project was inspired by the paper "Monadic Parser Combinators" by Hutton and Meijer, the paper "Parsec: Direct
Style Monadic Parser Combinators for the Real World" by Leijen and Meijer, and the chapter on parser combinators in the
book "Programming in Scala" by Odersky et al.
