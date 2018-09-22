
## Issue

Welcome to use [issue tracker](https://github.com/alibaba/arthas/issues) to give us :bowtie::

* feedbacks - what you would like to have;
* usage tips - what usages you have found splendid;
* experiences - how you use Arthas to do **effective** troubleshooting;

## Documentation


用户文档在`site/src/site/sphinx`目录下，如果希望改进Arthas用户文档，欢迎提交PR。

Welcome PR to further improve English [documentation](https://github.com/alibaba/arthas/tree/master/site/src/site/sphinx/en).

## Developer

Compilation requires JDK 7 and above since we are `java.lang.management.BufferPoolMXBean` while runtime requires JDK 6.

### Local Installation

Recommend to use [`as-package.sh`](as-package.sh) to package, which will auto-install the latest Arthas to local `~/.arthas` and when debugging, Arthas will auto-load the latest version.

F.Y.I
1. when using [`as.sh`](https://github.com/alibaba/arthas/blob/master/bin/as.sh) to start Arthas, it will get the latest version under `~/.arthas/lib`;
2. when [`as-package.sh`](as-package.sh) packaging, it will get the version from `pom.xml` and suffix it with the current timestamp e.g. `3.0.5.20180917161808`. 

You can also use `./mvnw clean package -DskipTests` to package and generate a `zip` under `packaging/target/` but remember when `as.sh` starts, it load the version under `~/.arthas/lib`.

### Start Arthas in specified version

When there are several different version, you can use `--use-version` to specify the version of Arthas to start your debug.

```bash
./as.sh --use-version 3.0.5.20180919185025
```

Tip: you can use `--versions` to list all available versions.

```bash
./as.sh --versions
```

### Debug

There are two stages when booting Arthas:

#### Stage 1

Execute `com.taobao.arthas.core.Arthas`, locate a proper JVM and attach to it. 

If you intend to debug this part, you can: 

```bash
./as.sh debug  pid
```

The JPDA port is `8888`, you can connect to it from remote to debug; if you want to make it suspend after start, you can:

```bash
JPDA_SUSPEND=y ./as.sh debug  pid
```

#### Stage 2
After attaching, Arthas agent runs inside the target process. If you want to debug agent, you have to make sure the target process is started in debug mode too. If so, then you can directly import the Arthas source code, and debug it. 

### Packaging All

* Arthas is using [Sphinx](http://www.sphinx-doc.org/en/master/) to generate the static site
* `sphinx-maven-plugin` configured in [`site/pom.xml`](https://github.com/alibaba/arthas/tree/master/site)
* `sphinx-maven-plugin` executes by downloading`sphinx-binary/`
* a [bug](https://github.com/rtfd/recommonmark/issues/93) in Sphinx plugin `recommonmark`; we fix it by packaging another [version](https://github.com/hengyunabc/sphinx-binary/releases/tag/v0.4.0.1)
* when packaging the whole project (Packaging All), you need to (Only Unix/Linux/Mac supported):

    ```bash
    ./mvnw clean package -DskipTests -P full -Dsphinx.binUrl=https://github.com/hengyunabc/sphinx-binary/releases/download/v0.4.0.1/sphinx.osx-x86_64
    ```



