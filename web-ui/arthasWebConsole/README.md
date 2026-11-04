# a Web client based on HTTP API for arthas

[English](./README.md)|[中文](README_ZH.md)

## usage

* Through clicking the button in the top right corner, you can quickly get or clear sessionId
* When you are in trouble, refreshing the page is a good way
* Some features must be used with sessionId. but some features must be used without sessionId
* In classloader module, you must select a classloader before using classloader to load class or resourse

## develop

* Strongly recommand devloping with vscode
* TS + Vue3 + Tailwindcss + xstate
* You can view the Graphical http requesting process with xstate
* The final bundle will be placed in `../target/static`

### notice

* When use pull_results, you can't use other cmd, such as ```sc class```.  
* The consoleMachine.ts will be replaced perRequestMachine.ts + pinia sooner or later
