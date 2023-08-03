Display, and update the vm diagnostic options.

`vmoption -h`{{execute T2}}

[vmoption command Docs](https://arthas.aliyun.com/en/doc/vmoption.html)

## Usage

### View all options

`vmoption`{{execute T2}}

### View individual option

`vmoption PrintGCDetails`{{execute T2}}

### Update individual option

`vmoption PrintGC true`{{execute T2}}

Then use the `vmtool` command to force GC, you can see the GC log printed in `Terminal 1`:

`vmtool --action forceGc`{{execute T2}}

### Configure print GC details

`vmoption PrintGCDetails true`{{execute T2}}

Then use the `vmtool` command to force GC, you can see the GC details printed in `Terminal 1`:

`vmtool --action forceGc`{{execute T2}}
