# Batch Processing

With the help of Batch Processing, you can run multiple commands in batch and get the final result at the end.
The process name can be specified using the “–select” parameter.

## Usage

### Step 1: Create the script

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

- Each command takes one line.
- Batch mode execution times (via `-n`) must be explicitly specified for `dashboard`, otherwise batch script cannot terminate.
- Commands such as `watch`/`tt`/`trace`/`monitor`/`stack` should include `-n` option to ensure the script can be able to quit.

A batch script also supports output redirection and background jobs; an `&` at the end of a command runs it in the background:

```bash
trace -n 1 Test t >> test.out
trace Test t >> test.out &
watch c.t.X test returnObj > &
```

The first command finishes after one execution and appends its result to `test.out`. The second command runs in the background and appends its results to the same file, while the script continues with the next command. The third command omits the redirection filename and writes its results to a log file under `~/logs/arthas-cache/`.

A background job with redirection writes its output to a file; without redirection, its output still appears in the batch result. `&` must be the last argument of the command, and batch scripts do not support end-of-line comments. See [asynchronous job](async.md) for more details.

### Step 2: Run the script

Use `-f` to specify the script file. By default the result will be output to the standard output, but you can redirect the output to the file like this:

```bash
./as.sh -f /var/tmp/test.as 56328 > test.out
```

Use `-c` also can specify the commands, like this:

```bash
./as.sh -c 'sysprop; thread' 56328 > test.out
```

### Step 3: Check the output

```bash
cat test.out
```
