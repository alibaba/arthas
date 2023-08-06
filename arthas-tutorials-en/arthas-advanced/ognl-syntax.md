## Indexing

- Array and List Indexing
  You can use `array["length"]` or `array["len" + "gth"]`.
- JavaBeans Indexed Properties  
  An object has a set of methods that follow the following pattern:
  ```java
  public PropertyType[] getPropertyName();
  public void setPropertyName(PropertyType[] anArray);
  public PropertyType getPropertyName(int index);
  public void setPropertyName(int index, PropertyType value);
  ```
  So `someProperty[2]` is equivalent to the Java code `getPropertyName(2)`.

`ognl "{1,2,3,4}[0]"`{{exec}}

The command above retrieves the first element of the list.

## Variable References

In OGNL, temporary variables can be defined using `#`. These variables are globally visible, and each step of the expression evaluation is saved in the variable `this`.

`ognl "{10,20,30}[0].(#this > 5 ? #this*2 : #this+10)"`{{exec}}

The command above retrieves the first element of the list and checks whether it is greater than `5`. If it is, it multiplies it by `2`. If it is not, it adds `10` to it.

## Calling Methods

`method( ensureLoaded(), name )`

Note:

- OGNL calls methods a little differently from the way Java does, because OGNL is interpreted and must choose the right method at run time, with no extra type information aside from the actual arguments supplied.
- In particular, a null argument matches all non-primitive types, and so is most likely to result in an unexpected method being called.

`ognl "{1,2,3,4}.size()"`{{exec}}

The command above can use the `size()` method of ArrayList to retrieve the size of the ArrayList.

## Chained Subexpressions

`headline.parent.(ensureLoaded(), name)`

is equivalent to

`headline.parent.ensureLoaded(), headline.parent.name`

`ognl "@java.lang.System@out.(print('Hello '), print('world\n'))"`{{exec}}

you can see the output "Hello world" in the terminal of `Tab1` after running the provided command.

## Collection Construction

### Lists

`ognl "1 in {2, 3}"`{{exec}}

This tests whether the number `1` is in the list or not.

### Native Arrays

`ognl "new int[] {1, 2, 3}"`{{exec}}

To create an array with all null or 0 elements, use the alternative size constructor

`ognl "new int[9]"`{{exec}}

### Maps

Create using a special syntax.

`ognl "#{ 'foo': 'foo value', 'bar': 'bar value' }"`{{exec}}

Create a Map of specified type.

`ognl "#@java.util.HashMap@{ 'foo': 'foo value', 'bar': 'bar value' }"`{{exec}}

### Projecting Across Collections

OGNL provides a simple way to call the same method or extract the same property from each element in a collection and store the results in a new collection. We call this "projection".

`ognl "{1, 2, 3}.{#this*2}"`{{exec}}

### Selecting From Collections

- Selecting all match  
  `ognl "{1024, 'Hello world!', true, 2048}.{? #this instanceof Number}"`{{exec}}

- Selecting first match  
  `ognl "{1024, 'Hello world!', true, 2048}.{^ #this instanceof Number}"`{{exec}}

- Selecting last match  
  `ognl "{1024, 'Hello world!', true, 2048}.{$ #this instanceof Number}"`{{exec}}

### Pseudo-Properties for Collections

OGNL defines some specific collection properties that have the same meaning as their corresponding Java collection methods.

- Collections
  - `size` The size of the collection
  - `isEmpty` Evaluates to `true` if the collection is empty
- List
  - `iterator` Evalutes to an Iterator over the List.
- Map
  - `keys` Evalutes to a Set of all keys in the Map
  - `values` Evaluates to a Collection of all values in the Map
- Set
  - `iterator` Evalutes to an Iterator over the Set

## Calling Constructors

The constructor of any class outside the `java.lang` package must be prefixed with the fully qualified name of the class

`ognl "new java.util.ArrayList()"`{{exec}}

## Calling Static Methods

`@class@method(args)`

## Getting Static Fields

`@class@field`

## Pseudo-Lambda Expressions

`ognl "#fact = :[#this<=1? 1 : #this*#fact(#this-1)], #fact(3)"`{{exec}}

The command implements a lambda function that recursively calculates a factorial function, and then calculates the factorial of 3.

## Note

For more detailed syntax, please refer to the [official documentation](https://commons.apache.org/proper/commons-ognl/language-guide.html)。
