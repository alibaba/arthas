下载
===

## 下载全量包

### 从Maven仓库下载

最新版本，点击下载：[![](https://img.shields.io/maven-central/v/com.taobao.arthas/arthas-packaging.svg?style=flat-square "Arthas")](http://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=com.taobao.arthas&a=arthas-packaging&e=zip&c=bin&v=LATEST)


如果下载速度比较慢，可以尝试用[阿里云的镜像仓库](https://maven.aliyun.com/)，比如要下载`3.x.x`版本（替换`3.x.x`为最新版本），下载的url是：

https://maven.aliyun.com/repository/public/com/taobao/arthas/arthas-packaging/3.x.x/arthas-packaging-3.x.x-bin.zip


### 从Github Releases页下载

[https://github.com/alibaba/arthas/releases](https://github.com/alibaba/arthas/releases)

### 用as.sh启动

解压后，在文件夹里有`as.sh`，直接用`./as.sh`的方式启动：

```bash
./as.sh
```

打印帮助信息：

```bash
./as.sh -h
```

### 用arthas-boot启动

或者在解压后，在文件夹里有`arthas-boot.jar`，直接用`java -jar`的方式启动：

```bash
java -jar arthas-boot.jar
```

打印帮助信息：

```bash
java -jar arthas-boot.jar -h
```



## 下载离线文档

下载文档：[![](https://img.shields.io/maven-central/v/com.taobao.arthas/arthas-packaging.svg?style=flat-square "Arthas")](http://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=com.taobao.arthas&a=arthas-packaging&e=zip&c=doc&v=LATEST)

