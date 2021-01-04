## FAQ


> For questions that are not in this list, please search in issues. [https://github.com/alibaba/arthas/issues](https://github.com/alibaba/arthas/issues)

##### How much impact does Arthas attach have on the performance of the original process?

[https://github.com/alibaba/arthas/issues/44](https://github.com/alibaba/arthas/issues/44)


##### How to view the result in `json` format

```bash
options json-format true
```

See more at [options](options.md)


##### Can arthas trace native methods

No.

##### Can arthas view the value of a variable in memory?

No. But you can use some tricks to intercept the object with the `tt` command, or fetch it from a static method.
