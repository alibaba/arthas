> dump java heap in hprof binary format, like `jmap`.

[heapdump command Docs](https://arthas.aliyun.com/en/doc/heapdump.html)

### Usage

#### Dump to file

`heapdump /tmp/dump.hprof`{{execute T2}}

#### Dump only live objects

`heapdump --live /tmp/dump.hprof`{{execute T2}}

#### Dump to tmp file

`heapdump`{{execute T2}}
