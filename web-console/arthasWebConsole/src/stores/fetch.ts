import { useInterpret, useMachine } from "@xstate/vue";
import { defineStore } from "pinia";
import { watchEffect } from "vue";
import { publicStore } from "./public";
import { waitFor } from "xstate/lib/waitFor";
import { interpret } from "xstate";
import permachine from "@/machines/perRequestMachine";
// 控制fetch的store
const getEffect = (
  M: ReturnType<typeof useMachine>,
  fn: (res: ArthasRes) => void,
) =>
  watchEffect(() => {
    if (M.state.value.context.response) {
      const response = M.state.value.context.response;
      fn(response as ArthasRes);
    }
  });
type Machine = ReturnType<typeof useMachine>;
type MachineService = ReturnType<typeof useInterpret>
export const fetchStore = defineStore("fetch", {
  state: () => ({
    sessionId: "",
    consumerId: "",
    requestId: "",
    online: false,
    wait: false,
    // 所有用pollingLoop都要
    jobRunning: false,
  }),
  getters: {
    getRequest: (state) =>
      (option: ArthasReq) => {
        /**
         * 对于never，就直接赋值为""，
         * 对于undefined, 就使用全局默认值
         * 对于定义的字符串，则使用定义的值 
         * @param key 
         * @returns 
         */
        const trans = (key:"sessionId"|"requestId"|"consumerId")=>{
          if(key in option) {
            console.log(option)
            //@ts-ignore
            if(option[key] !== undefined) {
            //@ts-ignore
              return option[key]
            } else {
              return state[key]
            }
          }
          return ""
        }
        let sessionId = trans("sessionId")
        let requestId = trans("requestId")
        let consumerId = trans("consumerId")
        const req = new Request("/api", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            ...option,
            sessionId,
            consumerId,
            requestId,
            // 若上面三个属性不传，直接用 as any而不是传undefined
          }),
        });
        return req;
      },
  },
  actions: {
    getPollingLoop(
      hander: Function,
      options: { step?: number; globalIntrupt?: boolean } = {
        step: 1000,
        globalIntrupt: false,
      },
    ) {
      let id = -1;
      const { step, globalIntrupt } = options;
      const that = this;
      return {
        // 自动轮询的可能会被错误打断
        open() {
          if (!this.isOn()) {
            if (globalIntrupt) that.jobRunning = true;
            hander();
            id = setInterval(
              (() => {
                if (
                  publicStore().isErr || (!that.jobRunning && globalIntrupt)
                ) {
                  this.close();
                } else {
                  hander();
                }
              }) as TimerHandler,
              step,
            );
          }
        },
        close() {
          if (this.isOn()) {
            if (globalIntrupt) that.jobRunning = false;
            clearInterval(id);
            id = -1;
          }
        },
        isOn() {
          return id !== -1;
        },
      };
    },
    pullResultsLoop(pollingM: Machine) {
      return this.getPollingLoop(
        () => {
          pollingM.send({
            type: "SUBMIT",
            value: {
              action: "pull_results",
              sessionId: undefined,
              consumerId: undefined,
            },
          });
        },
        {
          globalIntrupt: true,
        },
      );
    },
    onWait() {
      if (!this.wait) this.wait = true;
    },
    waitDone() {
      if (this.wait) this.wait = false;
    },
    getCommonResEffect(M: Machine, fn: (body: CommonRes["body"]) => void) {
      return getEffect(M, (res) => {
        if (Object.hasOwn(res, "body")) {
          fn((res as CommonRes).body);
        }
      });
    },
    /**
     * 注入enhancer:Proxy<Map<string,string[]>>
     */
    getPullResultsEffect(
      M: Machine,
      enhancer: Map<string, string[]>,
      fn: (result: ArthasResResult) => void,
    ) {
      return this.getCommonResEffect(M, (body: CommonRes["body"]) => {
        if (body.results.length > 0) {
          body.results.forEach((result) => {
            if (result.type === "enhancer") {
              enhancer.clear();
              enhancer.set("success", [result.success.toString()]);
              for (const k in result.effect) {
                enhancer.set(k, [result.effect[k as "cost"].toString()]);
              }
            }
            fn(result);
          });
        }
      });
    },
    interruptJob() {
      if (this.jobRunning) {
        const actor = interpret(permachine);
        console.log("'");
        actor.start();
        console.log("1212");
        actor.send("INIT");
        actor.send({
          type: "SUBMIT",
          value: {
            action: "interrupt_job",
            sessionId: this.sessionId,
          },
        });
      }
      this.jobRunning = false;
    },
    openJobRun() {
      this.jobRunning = true;
    },
    async isResult(m: MachineService) {
      return await waitFor(m, (state) => {
        console.log(state)
        return state.hasTag("result")
      });
    },
    tranOgnl(s: string): string[] {
      return s.replace(/\r\n\tat/g, "\r\n\t@").split("\r\n\t");
    },
    async baseSubmit(fetchM: MachineService, value: ArthasReq){
      fetchM.start()
      fetchM.send("INIT")
      fetchM.send({
        type: "SUBMIT",
        value
      })
      return this.isResult(fetchM).then(
        state=>{
          if (state.matches("success")) {
            return Promise.resolve<ArthasRes>(state.context.response)
          } else {
            return Promise.reject()
          }
        },
        err=>{
          console.log(err)
        }
      )
    }
  },
});
