Manual Installation
===================

### Download

**Latest `bin.zip`**: [![Arthas](https://img.shields.io/maven-central/v/com.taobao.arthas/arthas-packaging.svg?style=flat-square "Arthas")](http://search.maven.org/classic/#search%7Cga%7C1%7Cg%3A%22com.taobao.arthas%22%20AND%20a%3A%22arthas-packaging%22)


If the download is slow, try to download from the [repository in AliYun](https://maven.aliyun.com/); the downloading URL template is (remember to fill up the version `3.x.x` template to your needs)

`https://maven.aliyun.com/repository/public/com/taobao/arthas/arthas-packaging/3.x.x/arthas-packaging-3.x.x-bin.zip`
### Unzip

```bash
unzip arthas-packaging-bin.zip
```

### Install

```bash
sudo su admin
rm -rf /home/admin/.arthas/lib/* # remove all the leftover of the old outdated Arthas
cd arthas
./install-local.sh # switch the user based on the owner of the target Java process.
```

### Start

```bash
./as.sh # make sure the old Arthas has been shutdown (using command shutdown);
```
