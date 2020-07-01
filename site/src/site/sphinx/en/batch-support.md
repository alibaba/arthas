Batch Processing
================

With the help of Batch Processing, you can run multiple commands in batch and get the final result at the end.

### Usage

#### Step 1: Create the script

Create a `test.as` script suffixed with `as`. Here `as` is suggested for the suffix of the filename, but in fact any suffix is acceptable.

```bash
➜  arthas git:(develop) cat /var/tmp/test.as
help
dashboard -n 1
session
thread
sc -d org.apache.commons.lang.StringUtils
```

Note:
* Each command takes one line.
* Batch mode execution times (via `-n`) must be explicitly specified for `dashboard`, otherwise batch script cannot terminate.
* Commands such as `watch`/`tt`/`trace`/`monitor`/`stack` should include `-n` option to ensure the script can be able to quit.
* Also consider to use `async` (for example: `watch c.t.X test returnObj > &`) to put commands run at background and get the output from the log file, see more from [asynchronous job](async.md)

#### Step 2: Run the script

Use `-f` to specify the script file. By default the result will be output to the standard output, but you can redirect the output to the file like this:

```bash
./as.sh -f /var/tmp/test.as 56328 > test.out
```

Use `-c` also can specify the commands, like this:

```bash
./as.sh -c 'sysprop; thread' 56328 > test.out
```

#### Step 3: Check the output

```bash
cat test.out
```
