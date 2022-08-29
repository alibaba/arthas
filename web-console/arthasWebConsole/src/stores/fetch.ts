import { useMachine } from "@xstate/vue";
import { defineStore } from "pinia";
import { watchEffect } from "vue";
import { publicStore } from "./public";
import { waitFor } from 'xstate/lib/waitFor';
// 控制fetch的store
const getEffect = (M: ReturnType<typeof useMachine>, fn: (res: ArthasRes) => void) => watchEffect(() => {
  if (M.state.value.context.response) {
    const response = M.state.value.context.response
    fn(response as ArthasRes)
  }
})
type Machine = ReturnType<typeof useMachine>
export const fetchStore = defineStore("fetch", {
  state: () => ({
    sessionId: "",
    consumerId: "",
    requestId: "",
    online: false,
    wait:false
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
            hander()
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
    onWait(){
      if(!this.wait)this.wait = true
    },
    waitDone(){
      if(this.wait)this.wait = false
    },
    getCommonResEffect: (M: Machine, fn: (body: CommonRes["body"]) => void) =>{
      return getEffect(M,res=>{
        if(Object.hasOwn(res,"body")){
          fn((res as CommonRes).body)
        }
      })
    },
    
    interruptJob(M:Machine){
      M.send({
        type:"SUBMIT",
        value:{
          action:"interrupt_job",
          sessionId:this.sessionId,
        } 
      })
    },

    isReady(m:Machine){
      return waitFor(m.service,state=>state.matches("ready"))
    }

  },
});
