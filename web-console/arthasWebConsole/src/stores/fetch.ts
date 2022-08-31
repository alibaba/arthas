import { useMachine } from "@xstate/vue";
import { defineStore } from "pinia";
import { watchEffect } from "vue";
import { publicStore } from "./public";
import { waitFor } from "xstate/lib/waitFor";
import { interpret } from "xstate";
import permachine from "@/machines/perRequestMachine";
import { number } from "echarts";
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
        const req = new Request("/api", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            sessionId: state.sessionId,
            consumerId: state.consumerId,
            requestId: state.requestId,
            // 若上面三个属性不传，直接用 as any而不是传undefined
            ...option,
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
    onWait() {
      if (!this.wait) this.wait = true;
    },
    waitDone() {
      if (this.wait) this.wait = false;
    },
    getCommonResEffect: (M: Machine, fn: (body: CommonRes["body"]) => void) => {
      return getEffect(M, (res) => {
        if (Object.hasOwn(res, "body")) {
          fn((res as CommonRes).body);
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
    isReady(m: Machine) {
      return waitFor(m.service, (state) => state.matches("ready"));
    },
  },
});
