
> Memory compiler, compiles `.java` files into `.class` files in memory.

The classloader can be specified with the `-c`/`--classLoaderClass` option, the output directory can be specified with the `-d` option.

After compiling the `.class` file, you can use the `retransform` command to update the loaded classes in JVM.
