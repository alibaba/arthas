import { assign, createMachine, DoneInvokeEvent, spawn } from "xstate";
import { fetchStore } from "@/stores/fetch";
import { publicStore } from "@/stores/public";
import transformMachine from "./transformConfigMachine";

interface CTX {
  toObjM: typeof transformMachine | null;
  inputRaw: any;
  inputValue?: ArthasReq;
  request?: Request;
  response?: ArthasRes;
  resArr: (ArthasResResult | SessionRes | AsyncRes)[];
  err: string;
  // 暂时先anyscript
  publicStore?: any;
  fetchStore?: any;
}
type ET =
  | {
    type: "SUBMIT";
    value: ArthasReq | string;
  }
  | {
    type: "error.platform";
    data: any;
  }
  | {
    type: "done.invoke.getCommon";
    data: CommonRes;
  }
  | {
    type: "done.invoke.getSession";
    data: SessionRes;
  }
  | {
    type: "done.invoke.getAsync";
    data: AsyncRes;
  }
  | {
    type: "INIT";
  }
  | {
    type: "";
  };

const machine =
  /** @xstate-layout N4IgpgJg5mDOIC5QGMD2A7WqA2YB0AlhLgMQDKAqgEICyAkgCqKgAOqsBALgRsyAB6IAjAHYALHiEBmAKxiADADYATIoAcytaLEAaEAE9hQgJx5FQtWMUzjU+cvlq1AX2d60mHPmyoAhhAJ0KBIIDHxAgDdUAGt8DyxcPB9-QKgESNRkX24MAG15AF0+Ng4c9D5BBBl5ITNZMREZRUVjeWMtPUMEIXs8Y0UxLRF5KUtBmVd3DATvPwCgkjAAJyXUJbwWbGyAMzWAWzx4ryS51PT0KKyy-KKkEBKuHnK7yuraxXrG5tb2oU7EZSAvBOZrKERCGzKQbGESuNwgdCoCBwPhHRJEXDFdiPXgvRBiZT-bpSKSSeT2KRfKHGMQTeFo2YpIJY0pPCoAyx4OwNCEyERqeQKCFEnoiMzU8RiGFyRpSSYgBl4WAAV2QyDg8DuDzK7IQ6hkeChgLUUmUFkBLSJrT64KkihEgKU9rUsPp02O218BGwyqWYBZOOeoEq+sNBM0pvNqmMRLN8mBtlEIikYiEVhNrqmnkxWuxOrx3TUIrUeHJZfLFddriAA */
  createMachine({
    context: {
      toObjM: null,
      inputRaw: undefined,
      inputValue: undefined,
      request: undefined,
      response: undefined,
      publicStore: undefined,
      fetchStore: undefined,
      resArr: [],
      err: "",
    },
    schema: {
      context: {} as CTX,
      events: {} as ET,
      services: {} as {
        requestData: { data: any };
        stringToObj: { data: any };
      },
    },
    id: "console",
    initial: "idle",
    states: {
      idle: {
        on: {
          // 延迟pinia的挂载时机
          INIT: {
            target: "ready",
            actions: "initStore",
          },
        },
      },
      ready: {
        initial: "stringVal",
        states: {
          stringVal: {
            on: {
              SUBMIT: [{
                cond: "notString",
                actions: "rawInput",
                target: "objVal",
              }, {
                actions: [
                  "rawInput",
                  "toObj",
                ],
                target: "objVal",
              }],
            },
          },
          objVal: {
            entry: assign<CTX, ET>((ctx, e) => {
              return {
                inputValue: ctx.inputRaw,
              };
            }),
            always: [
              { cond: "notObj", target: "#failure" },
              {
                cond: "isAsync",
                target: "#asyncReq",
              },
              {
                cond: "isCommon",
                target: "#common",
              },
              {
                cond: "isSession",
                target: "#session",
              },
              {
                actions: "notReq",
                target: "#failure",
              },
            ],
            exit: "getReq",
          },
        },
      },
      common: {
        id: "common",
        tags: ["loading"],
        entry: "waitReq",
        invoke: {
          id: "getCommon",
          src: "requestData",
          onDone: [
            {
              cond: "cmdSucceeded",
              actions: ["transformRes"],
              target: "success",
            },
            {
              actions: ["setErrMessage"],
              target: "failure",
            },
          ],
          onError: [
            {
              actions: ["setErrMessage"],
              target: "failure",
            },
          ],
        },
        states: {},
      },
      session: {
        id: "session",
        tags: ["loading"],
        invoke: {
          id: "getSession",
          src: "requestData",
          onDone: [
            {
              cond: "cmdSucceeded",
              actions: ["transformSessionRes"],
              target: "success",
            },
            {
              actions: ["setErrMessage"],
              target: "failure",
            },
          ],
          onError: [
            {
              actions: ["setErrMessage"],
              target: "failure",
            },
          ],
        },
      },
      asyncReq: {
        id: "asyncReq",
        tags: ["loading"],
        entry: "waitReq",
        invoke: {
          id: "getAsync",
          src: "requestData",
          onDone: [
            {
              cond: "cmdSucceeded",
              actions: ["transformAsyncRes"],
              target: "success",
            },
            {
              actions: ["setErrMessage"],
              target: "failure",
            },
          ],
          onError: [
            {
              actions: ["setErrMessage"],
              target: "failure",
            },
          ],
        },
      },
      success: {
        entry: ["needReportSuccess", "renderRes"],
        tags: "result",
        always: {
          actions: ["reset"],
          target: "ready",
        },
      },
      failure: {
        id: "failure",
        tags: "result",
        entry: "outputErr",
        always: {
          actions: ["reset"],
          target: "ready",
        },
      },
      hist: {
        type: "history",
      },
    },
  }, {
    services: {
      requestData: async (context) => {
        const res = await fetch(context.request as Request);
        if (!res.ok) return Promise.reject("server error");
        return res.json();
      },
    },
    actions: {
      initStore: assign((context, event) => {
        if (event.type !== "INIT") return {};
        return {
          publicStore: publicStore(),
          fetchStore: fetchStore(),
        };
      }),
      rawInput: assign((context, event) => {
        if (event.type !== "SUBMIT") return {};
        return {
          inputRaw: event.value,
        };
      }),
      waitReq: (context) => {
        context.fetchStore.onWait();
      },
      getReq: assign((context, event) => {
        if (
          !context.inputValue || !context.fetchStore ||
          !("getRequest" in context.fetchStore)
        ) {
          return {};
        }
        return {
          request: context.fetchStore?.getRequest(context.inputValue),
          inputValue: context.inputValue,
        };
      }),
      transformRes: assign((context, event) => {
        if (event.type !== "done.invoke.getCommon") return {};

        return {
          response: event.data,
        };
      }),
      transformSessionRes: assign((context, event) => {
        if (event.type !== "done.invoke.getSession") return {};

        return {
          response: event.data,
        };
      }),
      transformAsyncRes: assign((ctx, e) => {
        if (e.type !== "done.invoke.getAsync") return {};
        return {
          response: e.data,
        };
      }),
      renderRes: assign((context, event) => {
        let resArr: (ArthasResResult | SessionRes | AsyncRes)[] =
          context.resArr;
        const response = context.response;
        if (!response) {
          return {};
        }
        if (
          Object.hasOwn(response, "body") &&
          Object.hasOwn((response as CommonRes).body, "results")
        ) {
          resArr = resArr.concat((context.response as CommonRes).body.results);
        } else {
          resArr = resArr.concat([response] as (SessionRes | AsyncRes)[]);
        }
        return { resArr: resArr.filter((v) => v && !Number.isNaN(v)) };
      }),
      outputErr: assign({
        err: (context, event) => {
          context.publicStore.$patch({
            isErr: true,
            ErrMessage: context.err,
          });
          return "";
        },
      }),
      // clearResArr: assign((context, event) => ({ resArr: [] })),
      toObj: assign((ctx) => {
        const m = spawn(transformMachine, { sync: true });
        m.send({
          type: "INPUT",
          data: ctx.inputRaw as string,
        });
        const s = m.getSnapshot();
        if (s?.matches("failure")) {
          return {
            inputRaw: undefined,
            err: s.context.err,
          };
        }
        let inputRaw:ArthasReq = {
          sessionId:undefined,
          ...s?.context.output
        } as ArthasReq
        return {
          inputRaw,
        };
      }),
      needReportSuccess: (context, e) => {
        if (context.inputValue?.action === "close_session") {
          context.fetchStore.$patch({
            sessionId: "",
            consumerId: "",
            online: false,
          });
          context.publicStore.$patch({
            isSuccess: true,
            SuccessMessage: `close session success!`,
          });
          return;
        }
        if (context.inputValue?.action === "init_session") {
          const response = (context.response as SessionRes);
          context.fetchStore.$patch({
            sessionId: response.sessionId,
            consumerId: response.consumerId,
            online: true,
          });
          context.publicStore.$patch({
            isSuccess: true,
            SuccessMessage: `init_session success!`,
          });
          return;
        }
        
        if (
          context.inputValue?.action === "exec" &&
          context.inputValue.command.includes("vmoption") &&
          context.inputValue.command !== "vmoption"
        ) {
          context.publicStore.$patch({
            isSuccess: true,
            SuccessMessage: JSON.stringify(
              (context.response as CommonRes).body.results,
            ),
          });
          return;
        }
      },
      setErrMessage: assign((_ctx, e) => {
        if (e.type === "SUBMIT" || e.type === "INIT" || e.type === "") {
          return {};
        }
        return { err: e.data as unknown as string };
      }),
      reset: (ctx, e) => {
        ctx.fetchStore.waitDone();
      },
      notReq: assign((context) => {
        return {
          err: "not request",
        };
      }),
    },
    guards: {
      // 判断命令是否有问题
      cmdSucceeded: (context, event) => {
        if (
          event.type !== "done.invoke.getCommon" &&
          event.type !== "done.invoke.getSession" &&
          event.type !== "done.invoke.getAsync"
        ) {
          return false;
        }
        if (["SCHEDULED", "SUCCEEDED"].includes(event.data.state)) {
          if (Object.hasOwn(event.data, "body")) {
            if (Object.hasOwn(event.data.body, "results")) {
              return (event.data as CommonRes).body.results.every((result) => {
                if (result.type === "status" && result.statusCode !== 0) {
                  return false;
                }
                if (
                  result.type === "message" &&
                  result.message ===
                    "all consumers are unhealthy, current job was interrupted."
                ) {
                  
                  return false;
                }
                if (
                  result.type === "options" &&
                  Object.hasOwn(result, "changeResult") &&
                  result.changeResult.afterValue === result.changeResult.beforeValue &&
                  (result.changeResult.afterValue as string).toString() !== (context.inputValue as CommandReq).command.split(" ")[2]
                ) {
                  console.log("?????")
                  // arthas 本身不会对 options抛错，得手动抛错
                  return false
                }
                return true;
              });
            } else {
              return ["READY", "TERMINATED"].includes(
                (event.data as AsyncRes).body.jobStatus,
              );
            }
          }
          // SessionRes
          return true;
        }

        if(context.inputValue && context.inputValue.action=== "interrupt_job") {
          /**
           * 永不拦截打断回收的错误
           */
          return true
        }
        return false;
      },
      isSession: (context) => {
        if (!context) return false;
        if (
          ["join_session", "init_session", "close_session", "interrupt_job"]
            .includes(context.inputValue!.action)
        ) {
          console.log("isSession");
          return true;
        }
        return false;
      },
      isCommon: (context) => {
        if (!context) return false;
        if (
          ["exec", "pull_results"]
            .includes(context.inputValue!.action)
        ) {
          console.log("isCommon");
          return true;
        }
        return false;
      },
      isAsync: (context) => {
        if (!context) return false;
        if (
          ["async_exec"]
            .includes(context.inputValue!.action)
        ) {
          console.log("isAsync");
          return true;
        }
        return false;
      },
      notObj: (ctx) => {
        if (ctx.inputValue) return false;
        return true;
      },
      // notReq: (context) => {
      //   if (context.inputValue) return true;
      //   return false;
      // },
      notString: (context, event) => {
        if (event.type !== "SUBMIT") return true;
        if (typeof event.value !== "string") return true;
        return false;
      },
    },
  });

export default machine;
