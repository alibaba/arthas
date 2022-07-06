sm
==

[`sm` online tutorial](https://arthas.aliyun.com/doc/arthas-tutorials?language=en&id=command-sm)

> Search method from the loaded classes.

`sm` stands for search method. This command can search and show method information from all loaded classes. `sm` can only view the methods declared on the target class, that is, methods from its parent classes are invisible.


### Options

|Name|Specification|
|---:|:---|
|*class-pattern*|pattern for class name|
|*method-pattern*|pattern for method name|
|`[d]`|print the details of the method|
|`[E]`|turn on regex matching while the default mode is wildcard matching|
|`[c:]`|The hash code of the special class's classLoader|
|`[classLoaderClass:]`| The class name of the ClassLoader that executes the expression. |
|`[n:]`|Maximum number of matching classes with details (100 by default)|

### Usage

View methods of `java.lang.String`:

```bash
$ sm java.lang.String
java.lang.String-><init>
java.lang.String->equals
java.lang.String->toString
java.lang.String->hashCode
java.lang.String->compareTo
java.lang.String->indexOf
java.lang.String->valueOf
java.lang.String->checkBounds
java.lang.String->length
java.lang.String->isEmpty
java.lang.String->charAt
java.lang.String->codePointAt
java.lang.String->codePointBefore
java.lang.String->codePointCount
java.lang.String->offsetByCodePoints
java.lang.String->getChars
java.lang.String->getBytes
java.lang.String->contentEquals
java.lang.String->nonSyncContentEquals
java.lang.String->equalsIgnoreCase
java.lang.String->compareToIgnoreCase
java.lang.String->regionMatches
java.lang.String->startsWith
java.lang.String->endsWith
java.lang.String->indexOfSupplementary
java.lang.String->lastIndexOf
java.lang.String->lastIndexOfSupplementary
java.lang.String->substring
java.lang.String->subSequence
java.lang.String->concat
java.lang.String->replace
java.lang.String->matches
java.lang.String->contains
java.lang.String->replaceFirst
java.lang.String->replaceAll
java.lang.String->split
java.lang.String->join
java.lang.String->toLowerCase
java.lang.String->toUpperCase
java.lang.String->trim
java.lang.String->toCharArray
java.lang.String->format
java.lang.String->copyValueOf
java.lang.String->intern
Affect(row-cnt:44) cost in 1342 ms.
```


View method `java.lang.String#toString` details:

```bash
$ sm -d java.lang.String toString
 declaring-class  java.lang.String
 method-name      toString
 modifier         public
 annotation
 parameters
 return           java.lang.String
 exceptions

Affect(row-cnt:1) cost in 3 ms.
```