import { defineStore } from "pinia";
import { Ref, ref, watch, watchEffect } from "vue";
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
    // 当初设计不好，应该写成setter，loop的意外错误关闭得挂到errdialog了。。。
    isErr: false,
    /**
     * isInput 是对input组件的锁，要使用inputVal，调用inputval的组件还要自定义一个锁
     */
    isInput: false,
    inputVal: "",
    /**
     * inputE是输入事件，true代表导入输入，false代表不导入输入
     */
    inputE: false,
    ErrMessage: "bug!!!",
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
    /**
     * @param inputRef 组件提供的值的存储区
     * @param getVal 从缓冲区到inputRef的处理流程
     * @param setVal 从inputRef到全局缓冲区的处理流程
     * @returns 独占缓冲区函数
     */
    inputDialogFactory<T = string>(
      inputRef: Ref<T>,
      getVal: (raw: string) => T,
      setVal: (input: Ref<T>) => string,
    ) {
      // 初始化一个响应式的锁
      let mutex = ref(false);
      // 发布订阅模式，把函数的闭包里的mutex和缓冲区的锁注册到vue上
      watchEffect(() => {
        if (mutex.value && !this.isInput) {
          //先上锁，防止再次触发该副作用
          mutex.value = false;
          // 把缓冲区的值输入到需要使用的组件里
          // 触发确认输入事件再输入
          if(this.inputE) inputRef.value = getVal(this.inputVal);
          // reset
          this.inputE = false;
          // 清空缓冲区
          this.inputVal = "";
        }
      });

      return () => {
        this.$patch({
          // 打开缓冲区
          isInput: true,
          // 把当前值导入到缓冲区
          inputVal: setVal(inputRef),
        });
        // 解锁
        mutex.value = true;
      };
    },
    numberCondition(
      raw: Ref<number>,
      scope: { min?: number; max?: number },
    ) {
      return {
        increase() {
          (scope.max === undefined || raw.value < scope.max) && raw.value++;
        },
        decrease() {
          (scope.min === undefined || raw.value > scope.min) && raw.value--;
        },
      };
    },
    nanoToMillis(nanoSeconds: number): number {
      return nanoSeconds / 1000000;
    },
  },
});
