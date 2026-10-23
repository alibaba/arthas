/**
 * This package is from https://github.com/async-profiler/async-profiler/
 * tag v4.6 commit 557276c2950c14a5ad7073e2bef5a16616309247
 *
 * <p>Arthas 仅集成命令使用的 AsyncProfiler、AsyncProfilerMXBean、Counter 和 Events，
 * 不引入独立的 Agent 入口或 Recording/Span API；常规采样、火焰图和 JFR 输出不依赖它们。
 * 4.6 已修复加载 native 库时主动初始化 Recording 导致的类加载器泄漏和死锁，
 * 但显式使用 Recording 仍会建立 JNI 全局引用；如需引入 Span API，须重新验证 ArthasClassloader 的释放。
 * 参见 <a href="https://github.com/async-profiler/async-profiler/issues/1811">#1811</a> 和
 * <a href="https://github.com/async-profiler/async-profiler/issues/1818">#1818</a>。
 */
package one.profiler;
