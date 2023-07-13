
show Mbean information

This command can show or monitor Mbean attribute information.

### Parameters

|  Name   | Specification  |
|  ----  | ----  |
|  name-pattern	|  pattern for the Mbean name |
|  attribute-pattern |  pattern for the attribute name |
|  [m]	|  show meta information |
|  [i:]	|  specify the interval to refresh attribute value (ms) |
|  [n:]	|  execution times |
|  [E]	|  turn on regex matching while the default mode is wildcard matching. Only effect on the attribute name |
	
## Usage

### Show all Mbean names

`mbean`{{execute T2}}

### Show meta data of Mbean

`mbean -m java.lang:type=Threading`{{execute T2}}

### Show attributes of Mbean

`mbean java.lang:type=Threading `{{execute T2}}

### Mbean name support wildcard matcher

`mbean java.lang:type=Th*`{{execute T2}}

Notes：ObjectName matching rules differ from normal wildcards, Reference resources：[javax.management.ObjectName](https://docs.oracle.com/javase/8/docs/api/javax/management/ObjectName.html?is-external=true)

### Multiple properties name matcher

Check memory pool:

`mbean java.lang:name=*,type=MemoryPool`{{execute T2}}

### Wildcards match specific attributes

`mbean java.lang:type=Threading *Count`{{execute T2}}

### Switch to regular matching using the `-E` command

`mbean -E java.lang:type=Threading PeakThreadCount|ThreadCount|DaemonThreadCount`{{execute T2}}

Check memory pool:

`mbean -E java.lang:name=*,type=MemoryPool Name|Usage|Type | grep " HEAP" -A3 -B1`{{execute T2}}

### Real-time monitoring using `-i` command

`mbean -i 1000 java.lang:type=Threading *Count`{{execute T2}}
