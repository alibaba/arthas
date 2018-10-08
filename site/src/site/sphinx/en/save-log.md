Log command outputs
===================

Log command outputs for later analysis, turned off by default.

To turn it on, using [options](options.md) as:

```bash
$ options save-result true
 NAME         BEFORE-VALUE  AFTER-VALUE
----------------------------------------
 save-result  false         true
Affect(row-cnt:1) cost in 3 ms.
```

F.Y.I 

1. logging file lies in: `{user.home}/logs/arthas-cache/result.log`;
2. remember to clean up the file to save disk space.

### Asynchronous log

 :notes: :notes: 
With the latest Arthas, you can log the outputs asynchronously in the background:

```bash
$ trace Test t >>  &
job id  : 2
cache location  : /Users/zhuyong/logs/arthas-cache/28198/2
```

F.Y.I

1. `quit/exit` or `Ctrl+C` will not affect the asynchronous jobs :sparkles:; 
2. default timeout for the background job is 1 day (you can set it via [options](options.md));
3. outputs will be save to the `cache location` now (no matter what `save-result` is - not `~/logs/arthas-cache/result.log` any more).

