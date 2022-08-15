import { defineStore } from "pinia";
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
          if (!this.isOn()) id = setInterval(hander, step);
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
