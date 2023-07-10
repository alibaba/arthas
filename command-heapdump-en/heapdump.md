
> dump java heap in hprof binary format, like `jmap`.


### Usage

#### Dump to file

`heapdump /tmp/dump.hprof`{{execute T2}}

```bash
[arthas@58205]$ heapdump /tmp/dump.hprof
Dumping heap to /tmp/dump.hprof...
Heap dump file created
```

#### Dump only live objects

`heapdump --live /tmp/dump.hprof`{{execute T2}}

```bash
[arthas@58205]$ heapdump --live /tmp/dump.hprof
Dumping heap to /tmp/dump.hprof...
Heap dump file created
```

#### Dump to tmp file

`heapdump`{{execute T2}}

```bash
[arthas@58205]$ heapdump
Dumping heap to /var/folders/my/wy7c9w9j5732xbkcyt1mb4g40000gp/T/heapdump2019-09-03-16-385121018449645518991.hprof...
Heap dump file created
```



