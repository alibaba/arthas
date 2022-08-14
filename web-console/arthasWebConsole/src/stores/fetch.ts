import { defineStore } from "pinia";
import { actions } from "xstate";
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
            ...option,
          }),
        });
        return req;
      },
  },
  actions: {
    getPollingLoop(hander: TimerHandler, step: number = 1000) {
      let id = -1;
      return {
        open() {
          id = setInterval(hander, step);
        },
        close() {
          if(id !== -1) clearInterval(id);
        },
      };
    },
  },
});
