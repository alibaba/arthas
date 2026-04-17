# Fix thread-unsafe LinkedHashMap in TimeTunnelCommand

## Problem

`timeFragmentMap` in `TimeTunnelCommand.java` is a plain `LinkedHashMap` accessed concurrently from multiple threads (advice listeners writing time fragments + command thread reading/iterating). This can cause `ConcurrentModificationException`, infinite loops in HashMap bucket chains, or silent data corruption.

## Root Cause

The field is declared as `new LinkedHashMap<Integer, TimeFragment>()` with a TODO comment acknowledging the thread-safety concern (`// TODO 并非线程安全？` — "not thread safe?"). The `AdviceListener` callbacks that populate this map run on instrumented application threads, while command operations (list, search, replay) run on the Arthas command thread.

## Fix

Replaced `new LinkedHashMap<>()` with `Collections.synchronizedMap(new LinkedHashMap<>())` to provide basic thread safety. Added the `java.util.Collections` import.

## Testing

- Run `tt` command while monitoring a high-throughput method with multiple threads calling it simultaneously.
- Previously this could cause `ConcurrentModificationException` during `tt -l` (list) operations; now it should be safe.

## Impact

Affects all Arthas users using the Time Tunnel (`tt`) command on multi-threaded applications. The bug is non-deterministic and depends on timing, making it difficult to reproduce consistently.
