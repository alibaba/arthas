watch
=====

Monitor methods in data aspect including `parameters`, `return values` and `exceptions`.

With the help of [OGNL](https://en.wikipedia.org/wiki/OGNL), you can easily check the details of variables when methods being invoked.

### Options

|Name|Specification|
|---:|:---|
|*class-pattern*|pattern for the class name|
|*method-pattern*|pattern for the method name|
|*expression*|expression to monitor|
|*condition-expression*|condition expression to filter|
|[b]|before method being invoked|
|[e]|when method encountering exceptions|
|[s]|when method exits normally|
|[f]|when method exits (either succeed or fail with exceptions)|
|[E]|turn on regex matching while the default is wildcard matching|
|[x:]|the depth to print the specified property with default value: 1|
|#cost|time cost|

**You should know:**
1. any valid OGNL expression as `"{params,returnObj}"` supported;
2. there are four *watching* points: `-b`, `-e`, `-s` and `-f` (the first three are off in default while `-f` on);
3. at the *watching* point, Arthas will use the *expression* to evaluate the variables and print them out;
4. `in parameters` and `out parameters` can be different since they can be modified within the invoked methods; `params` stands for `in parameters` in `-b`while `out parameters` in other *watching* points;
5. there are no `return values` and `exceptions` when using `-b`.
6. quoting rules: if there are quotes within the expression, use another type of quotes to quote the whole expression (single `''` or double `""` quotes). 

**Advanced:**
* [Critical fields in expression](advice-class.md)
* [Special usages](https://github.com/alibaba/arthas/issues/71)
* [OGNL official guide](https://commons.apache.org/proper/commons-ognl/language-guide.html)

### Usage

#### Check the `out parameters` and `return value`

```bash
$ watch demo.Demo addTwoLists "{params, returnObj}" -x 2 
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 42 ms.
ts=2018-09-29 19:19:19;result=@ArrayList[
    @Object[][
        @ArrayList[isEmpty=false;size=4],
        @ArrayList[isEmpty=false;size=2],
    
    ],
    @Integer[4],

]
```

#### Check `in parameters`

```bash
$ watch demo.Demo addTwoLists "{params, returnObj}" -x 2 -b
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 43 ms.
ts=2018-09-29 19:20:39;result=@ArrayList[
    @Object[][
        @ArrayList[isEmpty=false;size=2],
        @ArrayList[isEmpty=false;size=2],
    
    ],
    null,

]
```

Compared to the previous *check*, there are two differences:
1. size of `params[0]` is `2` instead of `4`;
2. `return value` is `null` since it's `-b`.


#### Check *before* and *after* at the same time

```bash
$ watch demo.Demo addTwoLists "{params, returnObj}" -x 2 -b -f
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 43 ms.
ts=2018-09-29 19:22:14;result=@ArrayList[
    @Object[][
        @ArrayList[isEmpty=false;size=2],
        @ArrayList[isEmpty=false;size=2],
    
    ],
    null,

]
ts=2018-09-29 19:22:14;result=@ArrayList[
    @Object[][
        @ArrayList[isEmpty=false;size=4],
        @ArrayList[isEmpty=false;size=2],
    
    ],
    @Integer[4],

]
```

**You should know:**
1. the first block of output is the *before watching* point;
2. the order of the output determined by the *watching* order itself (nothing to do with the order of the options `-b -s`).

#### Use `-x` to check more details

```bash
$ watch demo.Demo addTwoLists "{params, returnObj}" -x 3
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 45 ms.
ts=2018-09-29 19:23:04;result=@ArrayList[
    @Object[][
        @ArrayList[
            @String[a],
            @String[b],
            @String[c],
            @String[d],
        
        ],
        @ArrayList[
            @String[c],
            @String[d],
        
        ],
    
    ],
    @Integer[4],

]
```

#### Use condition expressions to locate specific call

```bash
$ watch demo.Demo addTwoLists "{params, returnObj}" "params.length==2" -x 3
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 42 ms.
ts=2018-09-29 19:24:59;result=@ArrayList[
    @Object[][
        @ArrayList[
            @String[a],
            @String[b],
            @String[c],
            @String[d],
        
        ],
        @ArrayList[
            @String[c],
            @String[d],
        
        ],
    
    ],
    @Integer[4],

]

# it's `-f` here in default, so `size()==4`
$ watch demo.Demo addTwoLists "{params, returnObj}" "params[0].size()==4" -x 3
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 46 ms.
ts=2018-09-29 19:25:29;result=@ArrayList[
    @Object[][
        @ArrayList[
            @String[a],
            @String[b],
            @String[c],
            @String[d],
        
        ],
        @ArrayList[
            @String[c],
            @String[d],
        
        ],
    
    ],
    @Integer[4],

]
```

#### Filter based on time cost

```bash
$ watch demo.Demo addTwoLists "{params, returnObj}" "params[0].size()==4" -x 3 #cost>30
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 46 ms.
ts=2018-09-29 19:29:05;result=@ArrayList[
    @Object[][
        @ArrayList[
            @String[a],
            @String[b],
            @String[c],
            @String[d],
        
        ],
        @ArrayList[
            @String[c],
            @String[d],
        
        ],
    
    ],
    @Integer[4],

]
```

**You should know:**
`#cost>30` (`ms`) filter out all invocations that take less than `30ms`.

