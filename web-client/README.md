## web-client
新一代的arthas web 客户端，使用目前主流的前端技术栈编写。
主要使用的技术栈如下:
1. [React](https://reactjs.org/)
2. [Typescript](https://www.typescriptlang.org/)
3. [tailwind css](https://tailwindcss.com/)

### 工程介绍

 - frontend 目录下是前端工程所在的目录。可供本地进行单独的前端测试。使用方法详见frontend下的README。

pom.xml中使用了一系列的plugin，最终会将前端资源编译到`target/templates`目录下

### 解决的问题
1. 工程化管理

相较于之前的形式，多了依赖管理和编译检查，能够有效提升代码可维护性。同时Typescript对于对有Java背景的使用者来说上手很快。

2. 提升代码可复用性

使用React，Typescript可以将页面和逻辑代码模块化，从而提升代码的可复用性。比如tunnel-server和core中页面的代码，可以有效得进行合并。

### 效果预览
1.首先在当前模块下执行`mvn clean:clean compile`， 确保target目录下有对应的资源生成。
2.运行test下面`DemoApplication`， 然后访问`http://localhost:8081`可以看到效果。

### 使用方式
web-client存在的最终形式是一个jar包，页面会编译到`target/templates`目录下，所需要的模块只要将web-client引入，并将http请求的路径指向classpath*:// 对应的资源文件即可。

### 目前实现的功能
1. core/resource下对应index.html功能已经完成了迁移