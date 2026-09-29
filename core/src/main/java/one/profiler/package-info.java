/**
 * This package is from https://github.com/async-profiler/async-profiler/
 * tag v4.5 commit 11aaea310cb75843f0e7532365d8cedc8d0e14c8
 *
 * <p>升级到 4.5 时有意不引入上游的 Recording 和 Span 类。Arthas 通过
 * AsyncProfiler 调用 native 库，不使用这两个类提供的 Java Span API；
 * 常规采样、火焰图和 JFR 输出不依赖它们。
 *
 * <p>4.5 的 native 库会在加载时尝试绑定 Recording，并持有该类的 JNI 全局引用，
 * 停止采样后也不释放，导致定义该类的 ArthasClassloader 无法回收。
 * Recording 不可见时 native 会跳过绑定，现有采样和输出功能仍可使用。
 * Span 依赖 Recording，因此一并省略，避免保留缺失依赖的 API。
 *
 * <p>详见 <a href="https://github.com/async-profiler/async-profiler/issues/1811">async-profiler #1811</a>。
 * 待上游修复引用释放问题且 Arthas 需要 Span API 时，再评估引入这两个类。
 */
package one.profiler;
