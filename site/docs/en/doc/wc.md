# wc

Count lines in command output through a pipe. Only line counting is supported; word counts, byte counts, and reading files directly are not supported.

## Options

| Option          | Description                                            |
| --------------- | ------------------------------------------------------ |
| `-l`, `--lines` | Count output lines; required when using a pipe         |
| `-h`, `--help`  | Show help by running `wc -h` or `wc --help` on its own |

## Examples

Count output lines:

```text
$ echo 'hello' | wc -l
1
```

Use the long option to count filtered output:

```bash
sysprop | grep java | wc --lines
```

Count the output of the thread command:

```bash
thread | wc -l
```

`wc` counts text output, including headers and separators, so the result is not a count of threads, classes, or method calls. The result is printed when the upstream command finishes. For commands that keep producing output, such as `watch`, use the upstream command's `-n` option to limit the number of executions.

::: warning
Counts for blank lines, text without a final newline, and similar cases may currently differ from the system `wc -l` command.
:::

## Help

Run either command on its own to view help, rather than on the right side of a pipe:

```bash
help wc
wc --help
```

Running `wc` or `wc -l` on its own does not read standard input; it displays a message explaining that a pipe is required.
