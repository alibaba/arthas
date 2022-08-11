import { defineStore } from "pinia";
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

  },
});
