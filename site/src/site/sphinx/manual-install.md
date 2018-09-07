手动安装Arthas
===

1. 从[这里](TODO)下载指定的版本, 以`3.0.20171103142340`这个版本为例。
```
wget TODO arthas-3.0.20171103142340-bin.zip
```

2. 解压缩arthas的压缩包
```
unzip arthas-3.0.20171103142340-bin.zip
```

3. 安装Arthas: 安装之前最好把所有老版本的Arthas全都删掉
```
sudo su admin
rm -rf /home/admin/.arthas/lib/*
cd arthas
./install-local.sh
```
> 注意，这里根据你需要诊断的Java进程的所属用户进行切换，例如集团规范是admin用户，而阿里云的可能是tomcat用户。否则会安装到个人目录下，这样的安装是无效的。
3. 启动Arthas: 启动之前，请确保老版本的Arthas已经shutdown.
```
./as.sh
```