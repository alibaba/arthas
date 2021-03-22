
按行统计输出结果

`jad demo.MathGame main`{{execute T2}}

```bash
$ jad demo.MathGame main

ClassLoader:
+-sun.misc.Launcher$AppClassLoader@70dea4e
  +-sun.misc.Launcher$ExtClassLoader@7a616cb1

Location:
/home/scrapbook/tutorial/math-game.jar

public static void main(String[] args) throws InterruptedException {
    MathGame game = new MathGame();
    while (true) {
        game.run();
        TimeUnit.SECONDS.sleep(1L);
    }
}

Affect(row-cnt:1) cost in 138 ms.
```

`jad demo.MathGame main | wc -l`{{execute T2}}

```bash
$ jad demo.MathGame main | wc -l
13
```
