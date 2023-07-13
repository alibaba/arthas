The `thread 1`{{execute interrupt}} command prints the stack of thread ID 1.

Arthas supports pipes, and you can find `main class` with `thread 1 | grep 'main('`{{execute T2}}.

You can see that `main class` is `demo.MathGame`:

```
$ thread 1 | grep 'main('
    at demo.MathGame.main(MathGame.java:17)
```
