import { defineStore } from "pinia";

export const transfromStore = defineStore("transformStore", {
  actions: {
    transformStackTrace(trace: StackTrace) {
      return `${trace.className}.${trace.methodName} (${trace.fileName}: ${trace.lineNumber})`;
    }
  },
});
