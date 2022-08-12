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
    }
  },
});
