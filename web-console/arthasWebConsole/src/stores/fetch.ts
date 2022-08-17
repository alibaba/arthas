import { defineStore } from "pinia";
import { publicStore } from "./public";
// 控制fetch的store
export const fetchStore = defineStore("fetch", {
  state: () => ({
    sessionId: "",
    consumerId: "",
    requestId: "",
    online: false,
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
    getPollingLoop(hander: Function, step: number = 1000) {
      let id = -1;
      return {
        open() {
          if (!this.isOn()) {
            id = setInterval(
              (() => {
                if (publicStore().isErr) {
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
            clearInterval(id);
            id = -1;
          }
        },
        isOn() {
          return id !== -1;
        },
      };
    },
  },
});
