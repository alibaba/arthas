
 `reset -h`{{execute T2}}

```bash
$ reset -h
 USAGE:
   reset [-h] [-E] [class-pattern]

 SUMMARY:
   Reset all the enhanced classes

 EXAMPLES:
   reset
   reset *List
   reset -E .*List

 OPTIONS:
 -h, --help                                                         this help
 -E, --regex                                                        Enable regular expression to match (wildcard matching by default)
 <class-pattern>                                                    Path and classname of Pattern Matching
 ```
 