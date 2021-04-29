/**
 * <pre>
 * copy from arthas-vmtool/src/main/java 。
 * 因为动态链接库只能被加载一次，只能使用一份代码。放在spy jar里保证只有一份。
 * TODO 当arthas本身版本升级时，已append 到bootstrap classloader的spy jar不能升级，VmTool的接口可以会调用失败。
 * </pre>
 */
package arthas;
