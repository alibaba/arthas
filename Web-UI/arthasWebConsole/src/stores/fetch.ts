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
type MachineService = ReturnType<typeof useInterpret>;
export const fetchStore = defineStore("fetch", {
  state: () => ({
    sessionId: "",
    consumerId: "",
    requestId: "",
    online: false,
    wait: false,
    /**
     * 需要给status计数，不然interupt自动关闭很容易出问题
     */
    statusZeroCount: 0,
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
        const trans = (key: "sessionId" | "requestId" | "consumerId") => {
          if (key in option) {
            //@ts-ignore
            if (option[key] !== undefined) {
              //@ts-ignore
              return option[key];
            } else {
              return state[key];
            }
          }
          return "";
        };
        let sessionId = trans("sessionId");
        let requestId = trans("requestId");
        let consumerId = trans("consumerId");
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
            //用于自动可自动打断，但是要加计时器
            if (globalIntrupt) that.jobRunning = false;
            clearInterval(id);
            id = -1;
          }
        },
        isOn() {
          return id !== -1;
        },
        /**
         * 无条件调用传入的hander
         */
        invoke() {
          hander()
        }
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
      fn: (result: ArthasResResult) => void,
    ) {
      return this.getCommonResEffect(M, (body: CommonRes["body"]) => {
        if (body.results.length > 0) {
          body.results.forEach((result) => {
            fn(result);
          });
        }
      });
    },
    interruptJob() {
      if (this.jobRunning) {
        // 先不管先后端同步的问题
        // dashboard可能会寄
        this.jobRunning = false;
        return this.baseSubmit(interpret(permachine), {
          action: "interrupt_job",
          sessionId: undefined,
        });
      }
      this.jobRunning = false;
      return Promise.reject("interrupt failured");
    },
    openJobRun() {
      this.jobRunning = true;
    },
    isResult(m: MachineService) {
      return waitFor(m, (state) => {
        return state.hasTag("result");
      });
    },
    tranOgnl(s: string): string[] {
      return s.replace(/\r\n\tat/g, "\r\n\t@").split("\r\n\t");
    },
    /**
     * @param fetchM 传入的服务
     * @param value 传入的请求
     * @returns 待处理的promise
     * 貌似不支持这样的类型推断,会出现分布式计算...以后想办法处理
     */
    baseSubmit<T extends BindQS>(fetchM: MachineService, value: T["req"]) {
      fetchM.start();
      fetchM.send("INIT");
      fetchM.send({
        type: "SUBMIT",
        value,
      });
      return this.isResult(fetchM).then(
        (state) => {
          if (state.matches("success")) {
            return Promise.resolve<T["res"]>(state.context.response);
          } else {
            return Promise.reject("ERROR");
          }
        },
        (err) => {
          return Promise.reject(err);
        },
      );
    },
    initSession() {
      return this.baseSubmit(interpret(permachine), {
        action: "init_session",
      });
    },
    asyncInit() {
      if (!this.online) {
        publicStore().ignore = true;
        return this.initSession().then(res=>{
          publicStore().ignore = false;
          return Promise.resolve(res)
        },err=>{
          publicStore().ignore = false;
          return Promise.reject(err)
        })
      }
      return Promise.resolve("alrealy init")
    },
  },
});
