# Tips for Complex OGNL Expressions

This page covers complex but production-friendly OGNL patterns in Arthas, including variables, collection selection and projection, temporary result construction, null handling, and ClassLoaders. Every complete command in the “Verified complex expressions” section was run in a real Arthas attach session against the repository's `math-game` JVM. Dynamic values such as thread names, counts, and object addresses will vary by environment.

Read the [`ognl` command documentation](ognl.md) for basic options and [Core variables in expressions](advice-class.md) for variables exposed by commands such as `watch`.

::: warning
OGNL runs inside the target JVM. A method call can read or modify application state. In production, generate and execute read-only expressions by default, and limit both the number of objects and the expansion depth. Do not invoke application methods such as `set*`, `add`, `remove`, or `clear`, and do not perform file, network, process, ClassLoader, privilege-escalating reflection, or thread-control operations. Calls to `put` in this page only modify a temporary `Map` created by the same expression.
:::

## Identify the expression context first

The grammar may be the same while the available variables are different. A common AI agent mistake is to copy context variables from another command into the standalone `ognl` command.

| Location                                                                                        | Available root objects or variables                                                                       | Common mistake                                                                        |
| ----------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------- |
| `ognl` command                                                                                  | Static members, constructors, literals, and self-defined `#variables`                                     | Using `params`, `target`, `returnObj`, or `instances`                                 |
| `getstatic` expression                                                                          | The static field value being read is the root object                                                      | Treating it as a standalone `ognl` expression with no root object                     |
| Observation or condition expressions in `watch`, `stack`, `monitor`, `tt`, and related commands | [`Advice`](advice-class.md) variables such as `params`, `target`, `returnObj`, `throwExp`, and `isReturn` | Reading a return value at method entry, or invoking `returnObj` on the exception path |
| Conditions in `trace` and related commands                                                      | Advice variables exposed by that command; some commands also provide `#cost`                              | Assuming that every command provides `#cost`                                          |
| `line --express` / `--condition`                                                                | Advice variables plus line-level variables such as `lineNumber`, `localVarMap`, and `#cost`               | Using `localVarMap` or `lineNumber` in another command                                |
| `vmtool --express`                                                                              | `instances`, the array returned by `getInstances`                                                         | Using `instances` in the standalone `ognl` command                                    |

Also distinguish a complete Arthas command from a raw expression parameter:

```bash
# The Arthas console needs the full command and outer single quotes
ognl '#value=@java.lang.System@getProperty("java.version"), #value'
```

When an MCP tool, HTTP API, or another client already provides a dedicated parameter named `expression`, `express`, or `conditionExpress`, pass only the raw expression. Do not add `ognl`, command options, outer single quotes, or a Markdown code fence:

```text
#value=@java.lang.System@getProperty("java.version"), #value
```

## Reliable syntax building blocks

| Purpose                                       | Syntax                                                            | Notes                                                                           |
| --------------------------------------------- | ----------------------------------------------------------------- | ------------------------------------------------------------------------------- |
| Define a temporary variable                   | `#name=value`                                                     | A custom variable must start with `#`                                           |
| Evaluate in sequence and return the last item | `#a=1, #b=2, #a+#b`                                               | Use commas, not semicolons; Arthas may treat a semicolon as a command separator |
| Access a static method or field               | `@fully.qualified.Class@method()`, `@fully.qualified.Class@FIELD` | Both `@` characters are required; prefer the fully qualified class name         |
| Create a temporary object                     | `new java.util.LinkedHashMap()`                                   | Do not include Java generics such as `new HashMap<String,Object>()`             |
| Create a List                                 | `{1, 2, 3}`                                                       | The result is a List, not a Java array                                          |
| Create a Map                                  | `#{"name":"arthas", "count":1}`                                   | Do not insert anything between `#` and `{`                                      |
| Create an array                               | `new java.lang.String[]{"a", "b"}`                                | The array type must be explicit                                                 |
| Project a collection                          | `items.{#this.getName()}`                                         | Evaluates each item and returns a new List                                      |
| Select from a collection                      | `items.{? #this != null}`                                         | Returns every matching item                                                     |
| Select the first or last match                | `items.{^ condition}`, `items.{$ condition}`                      | The result is still a List; account for an empty result before reading `[0]`    |
| Refer to the current item                     | `#this`                                                           | Its meaning changes with the current level in nested selections and projections |

For complex expressions, explicitly invoke public getters to reduce property-resolution and overload ambiguity. Prefer `#this.getName()` to `#this.name`. For maps, prefer `get("key")` and `containsKey("key")` to dotted property shorthand.

## Verified complex expressions

::: tip
Some examples in this section use `Thread.getAllStackTraces()` to obtain a collection in any demo JVM. This operation captures every thread stack. On a production JVM with many threads, prefer a dedicated command such as `thread`, or assess the cost first, and do not run these examples frequently.
:::

### Selection, projection, first match, and last match

The following expression defines a list and returns projection, selection, first-match, and last-match results together:

```bash
ognl -x 3 '#numbers={1,2,3,4,5,6}, #{"projection":#numbers.{#this * 10},"selection":#numbers.{? #this % 2 == 0},"first":#numbers.{^ #this % 2 == 0}[0],"last":#numbers.{$ #this % 2 == 0}[0]}'
```

Even numbers are known to exist in this literal, so `[0]` is safe here. For runtime collections, save the selected list and check whether it is empty:

```bash
ognl '#matched={1,3,5}.{^ #this % 2 == 0}, #matched.isEmpty() ? null : #matched[0]'
```

### Construct a structured result with temporary variables

Do not repeat expensive calls in a long expression. Save intermediate values, then return a newly created `LinkedHashMap`:

```bash
ognl -x 2 '#result=new java.util.LinkedHashMap(), #props=@java.lang.System@getProperties(), #threads=@java.lang.Thread@getAllStackTraces(), #result.put("javaVersion", #props.get("java.version")), #result.put("javaHome", #props.get("java.home")), #result.put("threadCount", #threads.size()), #result'
```

The returned keys are `javaVersion`, `javaHome`, and `threadCount`. Comma-separated subexpressions are evaluated in order, and the value of the whole expression is the final `#result`.

### Select first, then return only the required fields

```bash
ognl '@java.lang.Thread@getAllStackTraces().keySet().{? #this.isAlive() && !#this.isDaemon()}.{#this.getName()}'
```

`.{? ...}` returns the selected threads. The following `.{...}` projects only thread names, avoiding expansion of complete `Thread` objects.

### Count by group

The following expression counts threads by state. `#counts` is a temporary map created by this expression and does not modify an existing application object:

```bash
ognl -x 2 '#counts=new java.util.LinkedHashMap(), @java.lang.Thread@getAllStackTraces().keySet().{#state=#this.getState().toString(), #counts.put(#state, #counts.containsKey(#state) ? #counts.get(#state) + 1 : 1)}, #counts'
```

Example output:

```text
@LinkedHashMap[
    @String[RUNNABLE]:@Integer[10],
    @String[TIMED_WAITING]:@Integer[6],
    @String[WAITING]:@Integer[2],
]
```

Do not put Java Stream syntax such as `stream().filter(x -> ...)`, method references such as `Type::method`, or `computeIfAbsent(..., x -> ...)` into OGNL. OGNL does not parse Java lambdas. Use OGNL selection `.{? ...}` and projection `.{...}` instead.

### Bind `#this` early in nested objects

Inside the projection over `entrySet()`, `#this` is the current `Map.Entry`. Bind its key and value to meaningful variables before using them in a longer expression:

```bash
ognl '#all=@java.lang.Thread@getAllStackTraces(), #all.entrySet().{? #this.getKey().getName().startsWith("arthas")}.{#thread=#this.getKey(), #stack=#this.getValue(), #thread.getName() + " | " + #thread.getState() + " | depth=" + #stack.length}'
```

### Handle null explicitly

OGNL does not support JavaScript-style `?.`. Do not mix in SpEL `T(...)` or template syntax `${...}` either. Save a possibly null value and protect the method call with a conditional expression:

```bash
ognl '#value=@java.lang.System@getProperty("arthas.not.exists"), #value == null ? "<missing>" : #value.trim()'
```

Result:

```text
@String[<missing>]
```

### Handle both return and exception paths in `watch`

`-f` evaluates the expression after both a normal return and an exception. Branch on `isReturn` before accessing `returnObj` or `throwExp`:

```bash
watch demo.MathGame primeFactors '#result=new java.util.LinkedHashMap(), #result.put("number", params[0]), #result.put("outcome", isReturn ? "return" : "throw"), #result.put("detail", isReturn ? returnObj.size() : throwExp.getMessage()), #result' -f -n 2 -x 2
```

This expression evaluates on both paths in the real `math-game` JVM. `-n 2` limits the number of observations so the command terminates.

### Use `instances` in `vmtool`

`instances` exists only in the expression context of `vmtool --action getInstances --express`:

```bash
vmtool --action getInstances --className java.lang.Thread --limit 100 --express 'instances.{? #this.isAlive() && #this.getName().startsWith("arthas")}.{#this.getName()}'
```

Use `--limit` before selection and projection. Do not use `--limit -1` unconditionally for an application class that may have many instances.

## ClassLoader selection is not OGNL syntax

Before accessing an application class, identify its ClassLoader:

```bash
sc -d com.example.OrderService
```

Read `classLoaderHash` from the output and pass it as an `ognl` command option:

```bash
ognl -c <classLoaderHash> '@com.example.OrderService@STATIC_FIELD'
```

Keep these points in mind:

- `-c` and `--classLoaderClass` are Arthas command options, not parts of the OGNL expression.
- A class name may be loaded by multiple ClassLoaders. Do not guess the hash, and remember that it may change after a JVM restart.
- For `ClassNotFoundException`, inspect the ClassLoader before randomly rewriting OGNL syntax.
- `--classLoaderClass` is appropriate only when exactly one ClassLoader instance has that type. Use the hash when multiple instances match.

## Quoting and formatting: there are two parsers

Arthas parses the command line first, then OGNL parses the expression. A shell, JSON, or MCP transport adds another formatting layer.

- In the Arthas console, wrap a complete expression in single quotes and use double quotes for OGNL string literals.
- Keep the final command on one physical line. Visual terminal wrapping is harmless, but do not insert a real line break inside a class name, `.`, `@`, `#variable`, or an operator.
- Do not insert whitespace after `.`, as in the invalid form `#map. get("key")`.
- Join expression steps with commas, not semicolons.
- Keep outer single quotes around expressions containing `{$ ...}`, otherwise some shells will interpret `$`.
- For an MCP expression parameter, do not include Markdown backticks, outer single quotes, or the complete command.
- For JSON transport, escape double quotes according to JSON rules only. Do not copy shell backslash escaping verbatim into a raw expression parameter.

When quoting becomes deeply nested, prefer an Arthas [batch file](batch-support.md) or a dedicated expression field in the API to reduce shell-escaping layers.

## Do not classify every failure as a syntax error

| Symptom or exception keyword                             | Check first                                                                     | Recommended action                                                                                                                             |
| -------------------------------------------------------- | ------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| `Malformed OGNL expression`, `ExpressionSyntaxException` | Quotes, brackets, `@class@member`, commas, and projection/selection braces      | Reduce to a minimal expression, then add one fragment at a time                                                                                |
| `NoSuchPropertyException`                                | Whether the command exposes that variable; whether the property name is correct | Check the context-variable table and use an explicit public getter                                                                             |
| `MethodFailedException`, `NoSuchMethodException`         | Method name, argument types, and static versus instance invocation              | Use `sm`/`jad` to inspect the real signature instead of guessing an overload                                                                   |
| `ClassNotFoundException`                                 | ClassLoader                                                                     | Run `sc -d`, then specify `-c`                                                                                                                 |
| A null intermediate object                               | Missing null branch in a call chain                                             | Assign it to a `#variable` and guard the next call with a conditional expression                                                               |
| `stricter invocation mode`                               | The method or declaring class is blocked by OGNL strict mode                    | Do not disable the protection or bypass it with reflection; use `jvm`, `thread`, `sysprop`, or another dedicated command, or a safe public API |
| `module ... does not export`                             | JDK module boundaries, not necessarily OGNL grammar                             | Use a dedicated Arthas command or an accessible public application API                                                                         |
| Timeout, huge output, or target-JVM disturbance          | Expression work and expansion scope                                             | Select before projecting, avoid repeated calls, and set `--limit`, `-n`, and a reasonable `-x`                                                 |

For example, `@java.lang.Runtime@getRuntime()` can be rejected by the default OGNL strict invocation mode even though its syntax is valid. Repeatedly changing `@`, brackets, or quotes cannot fix this category of failure.

## Generation and validation protocol for AI agents

Consider adding the following constraints to prompts for agents that generate Arthas commands:

```text
You are generating OGNL for Arthas. Before generating it, you must confirm:
1. Whether the caller needs a complete Arthas command or a raw expression parameter. Output only the requested form.
2. Whether the expression runs in the ognl, getstatic, watch/stack/monitor/tt/trace, line, or vmtool context. Use only variables that actually exist in that context.
3. For application classes, run sc -d to identify the ClassLoader. If a method or field is uncertain, inspect it with sm/jad instead of guessing its name or overload.
4. Use @fully.qualified.Class@member for static access, #name for temporary variables, and commas for sequential steps.
5. Do not generate Java lambdas/Streams, method references, ?., SpEL T(...), ${...}, generic constructors, or semicolon statements.
6. Default to read-only behavior. Forbid process, file, network, ClassLoader, privilege-escalating reflection, thread-control, and application-state mutations. Mutation is allowed only on temporary collections created with new inside the expression.
7. Select before projecting, return only required fields, and set reasonable limit, -n, and -x values.
8. Keep the final expression on one physical line. Never break or insert whitespace inside a class name, dot access, @...@, #variable, or operator.

Validate incrementally instead of submitting one untested long expression:
A. Verify that the minimal root object, static field, or context variable is accessible.
B. Add only one #variable assignment, method call, selection, or projection per attempt, and execute it.
C. Add Map/List output construction and limiting options last.
D. On failure, classify it as command/escaping, grammar, context variable, ClassLoader, null, method signature, access restriction, or resource cost. Preserve the validated prefix and change only the failing fragment.
E. Claim that an expression is verified only after receiving a real Arthas result without a syntax exception.
```

Before returning output, the agent should also perform a mechanical check:

- Does every `@class@member` contain a matching pair of `@` characters?
- Are `()`, `[]`, `{}`, and quotes balanced?
- Does every custom variable start with `#`?
- Does `#this` refer to the intended nesting level in each selection or projection?
- Are possibly empty selections, map values, `returnObj`, and `throwExp` protected by a branch?
- Was a command option incorrectly placed inside the expression, or a complete command placed in a tool's expression field?
- Did automatic formatting split a class name, method name, or operator?

This process—confirm context, execute incrementally, then construct the final output—is more reliable than rewriting an entire complex expression after every failure, and it preserves evidence about the actual cause.

## Further reading

- [Arthas issue #71: special OGNL usages](https://github.com/alibaba/arthas/issues/71)
- [Apache Commons OGNL Language Guide](https://commons.apache.org/dormant/commons-ognl/language-guide.html)
