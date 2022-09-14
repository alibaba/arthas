import { defineStore } from "pinia";
import { watchEffect } from "vue";
import { useMachine } from "@xstate/vue";

const getEffect = (M: ReturnType<typeof useMachine>, fn: (res: ArthasRes) => void) => watchEffect(() => {
  if (M.state.value.context.response) {
    const response = M.state.value.context.response
    fn(response as ArthasRes)
  }
})

export const publicStore = defineStore("public", { // Public项目唯一id
  state: () => ({
    userMsg: {},
    isErr: false,
    /**
     * isInput 是对input组件的锁，要使用inputVal，调用inputval的组件还要自定义一个锁
     */
    isInput:false,
    inputVal:'',
    ErrMessage: "应该是出现bug",
    isSuccess: false,
    SuccessMessage: "bug!!!",
  }),
  getters: {
    getUserMsg: (state) => {
      return state.userMsg;
    },
  },
  actions: {
    getCommonResEffect: (M: ReturnType<typeof useMachine>, fn: (body: CommonRes["body"]) => void) =>{
      return getEffect(M,res=>{
        if(Object.hasOwn(res,"body")){
          fn((res as CommonRes).body)
        }
      })
    },
    interruptJob(M:ReturnType<typeof useMachine>){
      M.send({
        type:"SUBMIT",
        value:{
          action:"interrupt_job"
        } as AsyncReq
      })
    }
  },
});
