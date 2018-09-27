Batch Processing
================

With the help of `Batch Processing`, you can run several commands in one line and get the results.

### Usage

#### Step-1: Create the script

Creating a `test.as` script suffixed with `as` here for consistency (actually any suffix is acceptable).

```
➜  arthas git:(develop) cat /var/tmp/test.as
help
dashboard -b -n 1
session
thread
sc -d org.apache.commons.lang.StringUtils
```

Attention:
* each command takes each independent line;
* `dashboard` command should include `-b` to turn on batch mode and `-n` to ensure the script ends;
* commands as `watch/tt/trace/monitor/stack` should include `-n` option to ensure the script ends;
* [asynchronous](async.md) can also be used as `watch c.t.X test returnObj > &`;

#### Step-2: Run the script

Using `-b` to turn on script mode, and `-f` to run it and you can also *redirect* the output as:

```bash
./as.sh -b -f /var/tmp/test.as 56328 > test.out
```

#### Step-3: Check the outputs

```bash
cat test.out
```
