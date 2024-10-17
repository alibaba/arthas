# line

::: tip
观测当函数执行到指定位置时的数据状态
:::

让你能方便地观察到程序执行到指定位置时的数据状况。能观察到的范围为：`本地变量（局部变量）`、`入参`，如果是实例方法，还能观测到`当前对象`，通过编写 OGNL 表达式进行对应变量的查看。

## 参数说明

line 的参数如下

|                参数名称 | 参数说明                                                    |
|--------------------:|:--------------------------------------------------------|
|     _class-pattern_ | 类名表达式匹配                                                 |
|    _method-pattern_ | 函数名表达式匹配                                                |
|          _location_ | 行号(**LineNumber**)或者特殊行标识(**LineCode**)                 |
|           _express_ | 观察表达式，默认值：`varMap`                                      |
| _condition-express_ | 条件表达式                                                   |
|                [x:] | 指定输出结果的属性遍历深度，默认为 1，最大值是 4                              |

这里重点要说明的是**location**参数，它的作用是确定要在哪个位置进行观测，它的值有两种类型：

- **LineNumber**：行号，就是通俗意义中的源文件里的第几行，如下方的 `print(number, primeFactors);` 的**LineNumber**=**25**，其代表的含义是在**第25行**执行之前进行观测
```java
package demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class MathGame {
    private static Random random = new Random();

    private int illegalArgumentCount = 0;

    public static void main(String[] args) throws InterruptedException {
        MathGame game = new MathGame();
        while (true) {
            game.run();
            TimeUnit.SECONDS.sleep(1);
        }
    }

    public void run() throws InterruptedException {
        try {
            int number = random.nextInt() / 10000;
            List<Integer> primeFactors = primeFactors(number);
            print(number, primeFactors);

        } catch (Exception e) {
            System.out.println(String.format("illegalArgumentCount:%3d, ", illegalArgumentCount) + e.getMessage());
        }
    }
}
```
::: tip
**LineNumber**=**-1**时，则表示在函数结束前观测
:::

::: tip
无法使用**LineNumber**=**26**，因为该行号无法在编译后的class文件中找到
:::
- **LineCode**：特殊行标识，形如 **abcd-1**，它是由arthas生成的标识，只能通过`jad --lineCode`查看，详细使用可参考下方的 [使用LineCode进行观测](#使用linecode进行观测)

另外观察表达式是由 ognl 表达式组成，所以同watch命令类似，你也可以这样写`"{params,varMap}"`，只要是一个合法的 ognl 表达式，都能被正常支持。

观察的维度和watch有所差异，增加了`varMap`，但没有 `throwExp`、`returnObj`。

**特别说明**：

- 为什么需要用到**LineCode**呢？   

  因为在kotlin中，或者一些复杂的表达式中，不一定能以行号定位到期望观测的位置，而arthas生成的**LineCode**则可以提供更细一层级的定位。

- 为什么要限定在某函数内部来指定位置呢？

  首先绝大部分排查问题都是针对某具体函数的；
  其次若不指定函数，增强代码或者生成**LineCode**时可能需要遍历整个类，成本会比较高；
  再者在日常实践中，本地代码和arthas的宿主代码不一定一致，指定函数也能帮助用户尽早察觉。


## 使用参考

### 启动 Demo

启动[快速入门](quick-start.md)里的`math-game`。

### 观察函数执行到指定位置时的本地变量

::: tip
观察表达式，默认值是`varMap`
:::

```bash
$ line demo.MathGame run 25 -x 2
Press Q or Ctrl+C to abort.
Affect(class count: 1 , method count: 1) cost in 17 ms, listenerId: 2
method=demo.MathGame.run line=25
ts=2024-06-21 09:57:34.452; result=@HashMap[
    @String[primeFactors]:@ArrayList[
        @Integer[2],
        @Integer[7],
        @Integer[7],
        @Integer[991],
    ],
    @String[number]:@Integer[97118],
]
```

- 上面的结果里展示的是，当函数执行到第**25**行之前时，本地变量 `primeFactors` 和 `number` 的值

### 观察函数执行到指定位置时的参数和本地变量

```bash
$ line demo.MathGame run 25 "{params,varMap}" -x 2
Press Q or Ctrl+C to abort.
Affect(class count: 1 , method count: 1) cost in 16 ms, listenerId: 3
method=demo.MathGame.run line=25
ts=2024-06-21 10:02:07.295; result=@ArrayList[
    @Object[][isEmpty=true;size=0],
    @HashMap[
        @String[primeFactors]:@ArrayList[isEmpty=false;size=4],
        @String[number]:@Integer[44668],
    ],
]
```

- 同watch命令一致，我们可以通过`params`获取到参数值，因为该函数的参数为空，所以是`Object[]`

### 调整`-x`的值，观察具体的本地变量值

```bash
$ line demo.MathGame run 25 "{varMap}" -x 3
Press Q or Ctrl+C to abort.
Affect(class count: 1 , method count: 1) cost in 19 ms, listenerId: 4
method=demo.MathGame.run line=25
ts=2024-06-21 10:04:09.641; result=@ArrayList[
    @HashMap[
        @String[primeFactors]:@ArrayList[
            @Integer[2],
            @Integer[2],
            @Integer[3],
            @Integer[3],
            @Integer[17],
            @Integer[79],
        ],
        @String[number]:@Integer[48348],
    ],
]
```

- `-x`表示遍历深度，可以调整来打印具体的参数和结果内容，默认值是 1。
- `-x`最大值是 4，防止展开结果占用太多内存。用户可以在`ognl`表达式里指定更具体的 field。

### 条件表达式的例子

```bash
$ line demo.MathGame run 25 "{varMap}" "varMap[\"primeFactors\"][0]==2" -x 3
Press Q or Ctrl+C to abort.
Affect(class count: 1 , method count: 1) cost in 20 ms, listenerId: 12
method=demo.MathGame.run line=25
ts=2024-06-21 10:08:03.392; result=@ArrayList[
    @HashMap[
        @String[primeFactors]:@ArrayList[
            @Integer[2],
            @Integer[7],
            @Integer[31],
            @Integer[251],
        ],
        @String[number]:@Integer[108934],
    ],
]

```

- 只有满足条件的调用，才会有响应。

### 观察当前对象中的属性

如果想查看函数指定位置运行前，当前对象中的属性，可以使用`target`关键字，代表当前对象

```bash
$ line demo.MathGame run 25 'target'
Press Q or Ctrl+C to abort.
Affect(class count: 1 , method count: 1) cost in 18 ms, listenerId: 13
method=demo.MathGame.run line=25
ts=2024-06-21 10:10:00.761; result=@MathGame[
    random=@Random[java.util.Random@bebdb06],
    illegalArgumentCount=@Integer[1677],
]
```

然后使用`target.field_name`访问当前对象的某个属性

```bash
$ line demo.MathGame run 25 'target.illegalArgumentCount'
Press Q or Ctrl+C to abort.
Affect(class count: 1 , method count: 1) cost in 20 ms, listenerId: 14
method=demo.MathGame.run line=25
ts=2024-06-21 10:10:28.838; result=@Integer[1687]
```

### 使用LineCode进行观测
::: tip
当我们无法通过行号获得想要观测的位置时，才需要利用**LineCode**进行指定
:::

先使用 `jad --lineCode ` 来获取具体的映射位置

```bash
$ jad --lineCode demo.MathGame run
ClassLoader:                                                                                                                                                                                                                       
+-sun.misc.Launcher$AppClassLoader@18b4aac2                                                                                                                                                                                        
  +-sun.misc.Launcher$ExtClassLoader@1540e19d                                                                                                                                                                                      

Location:                                                                                                                                                                                                                          
/Users/xxxxx/IdeaProjects/github/arthas/math-game/target/classes/                                                                                                                                                               

       public void run() throws InterruptedException {
           try {
/*23*/         int number = random.nextInt() / 10000;
/*24*/         List<Integer> primeFactors = this.primeFactors(number);
/*25*/         MathGame.print(number, primeFactors);
           }
           catch (Exception e) {
/*28*/         System.out.println(String.format("illegalArgumentCount:%3d, ", this.illegalArgumentCount) + e.getMessage());
           }
       }

------------------------- lineCode location -------------------------
format： /*LineNumber*/ (LineCode)-> Instruction
/*23 */ (aacd-1)->  
                  invoke-method:java/util/Random#nextInt:()I
/*23 */ (5918-1)->  
                  assign-variable:e
/*24 */ (653f-1)->  
                  invoke-method:demo/MathGame#primeFactors:(I)Ljava/util/List;
/*24 */ (d961-1)->  
                  assign-variable:primeFactors
/*25 */ (416e-1)->  
                  invoke-method:demo/MathGame#print:(ILjava/util/List;)V
/*27 */ (5918-2)->  
                  assign-variable:e
/*28 */ (2455-1)->  
                  invoke-method:java/lang/StringBuilder#<init>:()V
/*28 */ (4076-1)->  
                  invoke-method:java/lang/Integer#valueOf:(I)Ljava/lang/Integer;
/*28 */ (b6e4-1)->  
                  invoke-method:java/lang/String#format:(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;
/*28 */ (850c-1)->  
                  invoke-method:java/lang/StringBuilder#append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
/*28 */ (a53d-1)->  
                  invoke-method:java/lang/Exception#getMessage:()Ljava/lang/String;
/*28 */ (850c-2)->  
                  invoke-method:java/lang/StringBuilder#append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
/*28 */ (f7bb-1)->  
                  invoke-method:java/lang/StringBuilder#toString:()Ljava/lang/String;
/*28 */ (2f1b-1)->  
                  invoke-method:java/io/PrintStream#println:(Ljava/lang/String;)V
Affect(row-cnt:1) cost in 103 ms.
```
上边结果中 `--- lineCode location ---` 分割线以下的就是**LineCode**定位相关的信息：
- `/*25 */` 表示该指令相邻的行号
- `(416e-1)` 表示特殊行标识，也就是**LineCode**，标识在具体的哪个位置进行观测
- `invoke-method: xxx` 则代表该指令是调用某个方法
- `assign-variable: xxx` 则代表该指令是给某个变量赋值

以下举例为在 `invoke-method:demo/MathGame#print:(ILjava/util/List;)V`执行前观测：
```bash
$ line demo.MathGame run 416e-1 -x 2
Press Q or Ctrl+C to abort.
Affect(class count: 1 , method count: 1) cost in 21 ms, listenerId: 16
method=demo.MathGame.run line=416e-1
ts=2024-06-21 10:37:50.531; result=@HashMap[
    @String[primeFactors]:@ArrayList[
        @Integer[5],
        @Integer[5],
        @Integer[6907],
    ],
    @String[number]:@Integer[172675],
]
```
- 通过 `jad --lineCode` 我们看到 **LineCode=416e-1** 位于 `invoke-method:demo/MathGame#print:(ILjava/util/List;)V` 上方，这就是我们观测的位置
- 另外结合行号、源码及指令顺序可知，`invoke-method:demo/MathGame#print:(ILjava/util/List;)V` 指令对应的源码是`print(number, primeFactors);`
