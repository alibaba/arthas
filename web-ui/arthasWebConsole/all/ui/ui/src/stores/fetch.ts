import { useInterpret, useMachine } from "@xstate/vue";
import { defineStore } from "pinia";
import { watchEffect } from "vue";
import { publicStore } from "./public";
import { waitFor } from "xstate/lib/waitFor";
import { interpret } from "xstate";
import permachine from "@/machines/perRequestMachine";
// 控制fetch的store
const getEffect = (
  M: ReturnType<typeof useMachine>,
  fn: (res: ArthasRes) => void,
) =>
  watchEffect(() => {
    if (M.state.value.context.response) {
      const response = M.state.value.context.response;
      //防止触发额外的副作用,因为输入时context会改变，导致多执行一次effect。。。
      M.state.value.context.response = undefined;
      fn(response as ArthasRes);
    }
  });
type Machine = ReturnType<typeof useMachine>;
type MachineService = ReturnType<typeof useInterpret>;
type PollingLoop = {
  open(): void;
  close(): void;
  isOn(): boolean;
  invoke(): void;
};
const nullLoop: PollingLoop = {
  open() {
  },
  close() {},
  isOn() {
    return false;
  },
  invoke() {},
};
export const fetchStore = defineStore("fetch", {
  state: () => ({
    sessionId: "",
    consumerId: "",
    requestId: "",
    online: false,
    wait: false,
    /**
     * 需要给status计数，不然interupt自动关闭很容易出问题
     */
    statusZeroCount: 0,
    // 所有用pollingLoop都要
    jobRunning: false,
    // 对于 pullresults可能会拉同一个结果很多次
    // jobIdSet: new Set<string>(),
    //由于轮询只会轮询一个命令，可以直接挂载当前的轮询机
    curPolling: nullLoop,
    //对session init以后4分钟重轮一次。
    // 如果curPolling是nullLoop,就要停掉状态维持来防止消耗异步请求结果的行为
  }),
  getters: {
    getRequest: (state) =>
      (option: ArthasReq) => {
        /**
         * 对于never，就直接赋值为""，
         * 对于undefined, 就使用全局默认值
         * 对于定义的字符串，则使用定义的值
         * @param key
         * @returns
         */
        const trans = (key: "sessionId" | "requestId" | "consumerId") => {
          if (key in option) {
            //@ts-ignore
            if (option[key] !== undefined) {
              //@ts-ignore
              return option[key];
            } else {
              return state[key];
            }
          }
          return "";
        };
        let sessionId = trans("sessionId");
        let requestId = trans("requestId");
        let consumerId = trans("consumerId");
        const req = new Request("/api", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            ...option,
            sessionId,
            consumerId,
            requestId,
            // 若上面三个属性不传，直接用 as any而不是传undefined
          }),
        });
        return req;
      },
  },
  actions: {
    /**
     * @param hander 要使用的函数
     * @param options step：间隔时间，globalIntrupt: 是否需要全局的打断按钮
     * @returns 一个轮询对象
     * 需要open以后才挂载到curPolling
     * close以后会重置为nullLoop
     * curloop的状态转移
     * nullloop->keepalive loop->polling loop
     */
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
      // 很有可能是keepalive session loop
      let preLoop = this.curPolling;
      const pollingLoop: PollingLoop = {
        // 自动轮询的可能会被错误打断
        open() {
          if (!this.isOn()) {
            // 切换为当前用到的 pollingLoop
            preLoop.close();
            that.curPolling = pollingLoop;

            if (globalIntrupt) that.jobRunning = true;
            hander();
            id = setInterval(
              (() => {
                if (
                  //isErr是瞬时的，点击了就会变回去。。。
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
            //用于自动可自动打断，但是要加计时器
            if (globalIntrupt) that.jobRunning = false;
            clearInterval(id);
            id = -1;
            // 重置为默认的nullloop
            that.curPolling = preLoop;
            // 继续keepalive
            preLoop.open();
          }
        },
        isOn() {
          return id !== -1;
        },
        /**
         * 无条件调用传入的hander
         */
        invoke() {
          hander();
        },
      };
      return pollingLoop;
    },
    pullResultsLoop(pollingM: Machine, globalIntrupt: boolean = true) {
      return this.getPollingLoop(
        () => {
          pollingM.send({
            type: "SUBMIT",
            value: {
              action: "pull_results",
              sessionId: undefined,
              consumerId: undefined,
            },
          });
        },
        {
          globalIntrupt,
        },
      );
    },
    /**
     * 用来提示是否正在请求中
     */
    onWait() {
      if (!this.wait) this.wait = true;
    },
    waitDone() {
      if (this.wait) this.wait = false;
    },
    getCommonResEffect(M: Machine, fn: (body: CommonRes["body"]) => void) {
      return getEffect(M, (res) => {
        if (Object.hasOwn(res, "body")) {
          fn((res as CommonRes).body);
        }
      });
    },
    /**
     * 注入enhancer:Proxy<Map<string,string[]>>
     */
    getPullResultsEffect(
      M: Machine,
      fn: (result: ArthasResResult) => void,
    ) {
      return this.getCommonResEffect(M, (body: CommonRes["body"]) => {
        if (body.results.length > 0) {
          body.results.forEach((result) => {
            // 对于既不是状态形状
            // 错误已经在machine那里拦截了
            fn(result);
          });
        }
      });
    },
    interruptJob() {
      if (this.jobRunning) {
        // 先不管先后端同步的问题
        // dashboard可能会寄
        this.jobRunning = false;
        return this.baseSubmit(interpret(permachine), {
          action: "interrupt_job",
          sessionId: undefined,
        });
      }
      this.jobRunning = false;
      return Promise.reject("There are not jobs running");
    },
    openJobRun() {
      this.jobRunning = true;
    },
    isResult(m: MachineService) {
      return waitFor(m, (state) => {
        return state.hasTag("result");
      });
    },
    tranOgnl(s: string): string[] {
      return s.replace(/\r\n\tat/g, "\r\n\t@").split("\r\n\t");
    },
    /**
     * @param fetchM 传入的服务
     * @param value 传入的请求
     * @returns 待处理的promise
     * 貌似不支持这样的类型推断,会出现分布式计算...以后想办法处理
     */
    baseSubmit<T extends BindQS>(fetchM: MachineService, value: T["req"]) {
      //防止在polling时触发其他命令
      if (this.jobRunning) return Promise.reject("there are jobs on running");
      fetchM.start();
      fetchM.send("INIT");
      fetchM.send({
        type: "SUBMIT",
        value,
      });
      return this.isResult(fetchM).then(
        (state) => {
          if (state.matches("success")) {
            return Promise.resolve<T["res"]>(state.context.response);
          } else {
            return Promise.reject("ERROR");
          }
        },
        (err) => {
          return Promise.reject(err);
        },
      );
    },
    keepaliveSession() {
      const kl = () => {
        let m = interpret(permachine);
        m.start();
        m.send("INIT");
        m.send({
          type: "SUBMIT",
          value: {
            action: "pull_results",
            sessionId: undefined,
            consumerId: undefined,
          },
        });
        
      };
      kl();
      let id = -1;
      let that = this;
      let loop = {
        open() {
          if (!this.isOn()) {
            // 切换为kl_loop
            that.curPolling.close();
            that.curPolling = loop;
            kl();
            id = setInterval(
              (() => {
                if (
                  // 不在线或者意外报错就停掉
                  publicStore().isErr || !that.online
                ) {
                  this.close();
                } else {
                  kl();
                }
              }) as TimerHandler,
              60_000,
            );
          }
        },
        close() {
          if (this.isOn()) {
            clearInterval(id);
            id = -1;
            // 重置为默认的nullloop
            that.curPolling = nullLoop;
          }
        },
        isOn() {
          return id !== -1;
        },
        invoke() {},
      };
      // 会先运行一次，当有任务执行
      loop.open();
      return loop;
    },
    initSession() {
      let p1 = this.baseSubmit(interpret(permachine), {
        action: "init_session",
      }).then((res) => {
        // 自动调度,维持session活性
        this.keepaliveSession();
      });
      return p1
    },
    asyncInit() {
      if (!this.online) {
        publicStore().ignore = true;
        return this.initSession().then((res) => {
          publicStore().ignore = false;
          return Promise.resolve(res);
        }, (err) => {
          publicStore().ignore = false;
          return Promise.reject(err);
        });
      }
      return Promise.resolve("alrealy init");
    },
    closeSession() {
      return this.baseSubmit(interpret(permachine), {
        action: "close_session",
        sessionId: undefined,
      });
    },
  },
});
