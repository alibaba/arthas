## 索引

- 数组和列表的索引  
  可以使用 `array["length"]`{{}} 或 `array["len" + "gth"]`{{}}
- JavaBeans 索引属性  
   如一个 JavaBeans 有以下四个重载方法：
  ```java
  public PropertyType[] getPropertyName();
  public void setPropertyName(PropertyType[] anArray);
  public PropertyType getPropertyName(int index);
  public void setPropertyName(int index, PropertyType value);
  ```
  则`someProperty[2]`{{}} 等价于 Java 代码的 `getPropertyName(2)`{{}}

`ognl "{1,2,3,4}[0]"`{{exec}}

通过上面命令可以获取到列表的第一个元素

## 变量引用

使用 `#`{{}} 在 OGNL 中定义临时变量，他们全局可见，此外表达式计算的每一步结果都保存在变量 `this`{{}} 中：

`ognl "{10,20,30}[0].(#this > 5 ? #this*2 : #this+10)"`{{exec}}

上面命令通过获取列表的第一个元素进行判断如果大于 `5`{{}} 则乘以 `2`{{}} 反之则加 `10`{{}} 。

## 方法调用

`method( ensureLoaded(), name )`{{}}

注意：

- OGNL 是运行时调用，因此没有任何静态类型的信息可以参考，所以如果解析到有多个匹配的方法，则任选其中一个方法调用
- 常量 null 可以匹配所有的非原始类型的对象

`ognl "{1,2,3,4}.size()"`{{exec}}

通过上面命令可以调用 `ArrayList`{{}} 的 `size()`{{}} 方法获取到 ArrayList 的大小

## 复杂链式表达式

`headline.parent.(ensureLoaded(), name)`{{}}

等价于

`headline.parent.ensureLoaded(), headline.parent.name`{{}}

`ognl "@java.lang.System@out.(print('Hello '), print('world\n'))"`{{exec}}

运行上面这个命令后，你可以在 Tab1 的终端看到 `Hello world`{{}} 的输出。

## 集合操作

### 新建列表

`ognl "1 in {2, 3}"`{{exec}}

上面这条命令判断 `1`{{}} 是否在列表 `[2, 3]`{{}} 中。

### 新建原生数组

`ognl "new int[] {1, 2, 3}"`{{exec}}

指定长度

`ognl "new int[9]"`{{exec}}

### 新建 Maps

新建普通 Map

`ognl "#{ 'foo': 'foo value', 'bar': 'bar value' }"`{{exec}}

新建特定类型 Map

`ognl "#@java.util.HashMap@{ 'foo': 'foo value', 'bar': 'bar value' }"`{{exec}}

### 集合的投影

OGNL 把对针对集合上的每个元素调用同一个方法并返回新的集合的行为称之为“投影”。

`ognl "{1, 2, 3}.{#this*2}"`{{exec}}

### 查找集合元素

- 查找所有匹配的元素  
  `ognl "{1024, 'Hello world!', true, 2048}.{? #this instanceof Number}"`{{exec}}

- 查找第一个匹配的元素  
  `ognl "{1024, 'Hello world!', true, 2048}.{^ #this instanceof Number}"`{{exec}}

- 查找最后一个匹配的元素  
  `ognl "{1024, 'Hello world!', true, 2048}.{$ #this instanceof Number}"`{{exec}}

### 集合的虚拟属性

OGNL 定义了一些特定的集合属性，含义与相应的 Java 集合方法完全等价。

- Collections
  - `size`{{}} 集合大小
  - `isEmpty`{{}} 集合为空时返回 `true`{{}}
- List
  - `iterator`{{}} 返回 List 的 iterator
- Map
  - `keys`{{}} 返回 Map 的所有 Key 值
  - `values`{{}} 返回 Map 的所有 Value 值
- Set
  - `iterator`{{}} 返回 Set 的 iterator

## 构造函数

非 `java.lang`{{}} 包下的所有类的构造函数都要用类的权限定名称。

`ognl "new java.util.ArrayList()"`{{exec}}

## 静态方法

`ognl -x 3 '@java.lang.Math@sqrt(9.0D)'`{{exec}}

## 静态属性

`ognl -x 3 '@java.io.File@separator'`{{exec}}

## 伪 lambda 表达式

`ognl "#fact = :[#this<=1? 1 : #this*#fact(#this-1)], #fact(3)"`{{exec}}

该命令实现了一个 lambda 递归实现了一个阶乘函数，并求 3 的阶乘。

## 补充

更多详细语法请查看[官方文档](https://commons.apache.org/proper/commons-ognl/language-guide.html)。
