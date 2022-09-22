import { defineStore } from "pinia";
import { nextTick, Ref, ref, watch, watchEffect } from "vue";
import { useMachine } from "@xstate/vue";

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

export const publicStore = defineStore("public", { // Public项目唯一id
  state: () => ({
    userMsg: {},
    isErr: false,
    /**
     * isInput 是对input组件的锁，要使用inputVal，调用inputval的组件还要自定义一个锁
     */
    isInput: false,
    inputVal: "",
    ErrMessage: "应该是出现bug",
    isSuccess: false,
    SuccessMessage: "bug!!!",
    isWarn: false,
    warnMessage: "",
    warningFn: () => {},
    /**
     * 忽略弹窗
     */
    ignore: false,
  }),
  getters: {
    getUserMsg: (state) => {
      return state.userMsg;
    },
  },
  actions: {
    getCommonResEffect: (
      M: ReturnType<typeof useMachine>,
      fn: (body: CommonRes["body"]) => void,
    ) => {
      return getEffect(M, (res) => {
        if (Object.hasOwn(res, "body")) {
          fn((res as CommonRes).body);
        }
      });
    },
    interruptJob(M: ReturnType<typeof useMachine>) {
      M.send({
        type: "SUBMIT",
        value: {
          action: "interrupt_job",
        } as AsyncReq,
      });
    },
    inputDialogFactory<T = string>(
      inputRef: Ref<T>,
      getVal: (raw: string) => T,
      setVal: (input: Ref<T>) => string,
    ) {
      let mutex = ref(false);
      watchEffect(() => {
        if (mutex.value && !this.isInput) {
          /**
           * 先上锁，防止再次触发该副作用
           */
          mutex.value = false;
          inputRef.value = getVal(this.inputVal);
          this.inputVal = "";
        }
      });
      return () => {
        this.$patch({
          isInput: true,
          inputVal: setVal(inputRef),
        });
        mutex.value = true;
      };
    },
    nanoToMillis(nanoSeconds: number): number {
      return nanoSeconds / 1000000;
    },
  },
});
