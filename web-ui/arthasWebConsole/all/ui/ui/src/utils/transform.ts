export default function transformStackTrace(trace: StackTrace) {
  return `${trace.className}.${trace.methodName} (${trace.fileName}: ${trace.lineNumber})`;
}