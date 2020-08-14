
> 查看已加载类的方法信息

“Search-Method” 的简写，这个命令能搜索出所有已经加载了 Class 信息的方法信息。

`sm` 命令只能看到由当前类所声明 (declaring) 的方法，父类则无法看到。

### 参数说明

|参数名称|参数说明|
|---:|:---|
|*class-pattern*|类名表达式匹配|
|*method-pattern*|方法名表达式匹配|
|[d]|展示每个方法的详细信息|
|[E]|开启正则表达式匹配，默认为通配符匹配|
|`[c:]`|指定class的 ClassLoader 的 hashcode|
|`[n:]`|具有详细信息的匹配类的最大数量（默认为100）|

### 使用参考

* 查找`java.lang.String`类的具体方法

`sm java.lang.String`{{execute T2}}

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

* 指定ClassLoader

查找ClassLoaderHash：

`sc -d demo.MathGame | grep classLoaderHash`{{execute T2}}

```bash
$ sc -d demo.MathGame | grep classLoaderHash
 classLoaderHash   70dea4e
```

记住这里的classLoaderHash：`70dea4e`，并用其替换`<classLoaderHash>`，手动执行下一条命令。

查找classloader下的class的method

`sc -c <classLoaderHash> -d demo.MathGame`

```bash
$ sm -c 70dea4e demo.MathGame
demo.MathGame <init>()V
demo.MathGame primeFactors(I)Ljava/util/List;
demo.MathGame main([Ljava/lang/String;)V
demo.MathGame run()V
demo.MathGame print(ILjava/util/List;)V
Affect(row-cnt:5) cost in 2 ms.
```

* 查找`java.lang.String#toString`函数并打印详细信息

`sm -d java.lang.String toString`{{execute T2}}

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
