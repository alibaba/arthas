
The commands in Arthas for finding loaded classes.

### sc

The `sc` command finds all the classes that the JVM has loaded.

When search an interface, it also search all implementation classes. For example, look at all the `Filter` implementation classes:

`sc javax.servlet.Filter`{{execute T2}}

With the `-d` option, it will print out the specific information of the loaded classes, which is very convenient for finding the class loading problem.

`sc -d javax.servlet.Filter`{{execute T2}}

`sc` supports wildcards, such as searching for all `StringUtils`:

`sc *StringUtils`{{execute T2}}

### sm

The `sm` command find the specific method of the class. such as:

`sm java.math.RoundingMode`{{execute T2}}

With `-d` option, it will print the deatils of the method.

`sm -d java.math.RoundingMode`{{execute T2}}

Find specific methods, such as the constructors:

`sm java.math.RoundingMode <init>`{{execute T2}}

