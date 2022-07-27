import machine from "@/machines/consoleMachine";
// import { interpret, Interpreter } from "xstate";
// import { done } from "xstate/lib/actions";
describe("test consoleMachine or command",()=>{
  // 由于store里用到fetch api node18 才有，并且fetch时用到代理。
  // 尽管可以使用mock，但对前后端的fetch就暂时不测了
  // it("version",()=>{
  //   const service = interpret(machine).onTransition(state=>{
  //     if(state.matches("success")) {
  //       done("ok")
  //     }
  //   })

  //   service.start()

  //   const vCmd = JSON.stringify({
  //     "action": "exec",
  //     "command": "version",
  //   })
  //   service.
  //     send({
  //       type: "SUBMIT",
  //       value: vCmd
  //     })
  // })
})