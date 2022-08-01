import { assign, createMachine, sendUpdate, spawn } from "xstate";
import { fetchStore } from "@/stores/fetch";
import { publicStore } from "@/stores/public";
import transformMachine from "./transformConfigMachine";
import { send } from "xstate/lib/actions";

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
    type: "INIT";
  }
  | {
    type: "CLEAR_RESARR";
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
        // on: {
        //   CLEAR_RESARR: { target: "ready", actions: "clearResArr" },
        // },
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
                  "toObj"
                ],
                target:"objVal"
              }],
            },
          },
          objVal: {
            entry: assign<CTX, ET>((ctx, e) => {
              console.log(ctx);
              return {
                inputValue: ctx.inputRaw,
              };
            }),
            always: [
              {
                cond: "notSession",
                target: "#common",
              },
              {
                cond: "notReq",
                target: "#session",
              },
              {
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
              target: "failure",
            },
          ],
          onError: [
            {
              target: "failure",
            },
          ],
        },
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
              target: "failure",
            },
          ],
          onError: [
            {
              target: "failure",
            },
          ],
        },
      },
      success: {
        entry: "renderRes",
        always: "ready",
      },
      failure: {
        id: "failure",
        entry: "returnErr",
        always: "ready",
      },
    },
  }, {
    services: {
      requestData: (context) =>
        fetch(context.request as Request).then(
          (res) => res.json(),
          (err) => Promise.reject(err),
        ),
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
        console.log(event);
        if (event.type !== "SUBMIT") return {};
        return {
          inputRaw: event.value,
        };
      }),
      getReq: assign((context, event) => {
        console.log("getReq exit", context.inputValue);

        if (!context.inputValue ||!context.fetchStore ||
          !("getRequest" in context.fetchStore)
        ) {
          return {};
        }
        const option = {} as any;
        Object.entries(context.inputValue).forEach(([k, v]) => {
          console.log(k,v)
          if (v) option[k] = v;
        });
        return {
          request: context.fetchStore?.getRequest(option),
          inputValue: option as ArthasReq,
        };
      }),
      transformRes: assign({
        response: (context, event) => {
          if (event.type !== "done.invoke.getCommon") return undefined;
          return event.data;
        },
      }),
      transformSessionRes: assign((context, event) => {
        if (event.type !== "done.invoke.getSession") return {};
        console.log(event.data);
        context.fetchStore?.$patch({
          sessionId: ("sessionId" in event.data) ? event.data.sessionId : "",
          consumerId: ("consumerId" in event.data) ? event.data.consumerId : "",
        });
        if (context.inputValue?.action === "init_session") {
          context.fetchStore.online = true;
          context.publicStore.$patch({
            isSuccess: true,
            SuccessMessage: `init_session success!`,
          });
        }
        if (context.inputValue?.action === "close_session") {
          context.fetchStore.online = false;
          context.publicStore.$patch({
            isSuccess: true,
            SuccessMessage: `close session success!`,
          });
        }
        return {
          response: event.data,
        };
      }),
      renderRes: assign((context, event) => {
        let resArr: (ArthasResResult | SessionRes | AsyncRes)[] =
          context.resArr;
        const response = context.response;
        if (!response) {
          return {};
        }
        console.log(response, "renderRes", context.resArr);
        if (
          Object.hasOwn(response, "body") &&
          Object.hasOwn((response as CommonRes).body, "results")
        ) {
          // 估计是ts的问题
          resArr = resArr.concat((context.response as CommonRes).body.results);
        } else {
          resArr = resArr.concat([response] as (SessionRes | AsyncRes)[]);
        }
        return { resArr: resArr.filter((v) => v && !Number.isNaN(v)) };
      }),
      returnErr: assign({
        err: (context, event) => {
          if (
            event.type !== "error.platform" &&
            event.type !== "done.invoke.getCommon" &&
            event.type !== "done.invoke.getSession"
          ) {
            return "";
          }
          context.publicStore?.$patch({ isErr: true, ErrMessage: event.data });
          return event.data;
        },
      }),
      // clearResArr: assign((context, event) => ({ resArr: [] })),
      toObj: assign((ctx) => {
        const m = spawn(transformMachine,{sync:true});
        m.send({
          type: "INPUT",
          data: ctx.inputRaw as string,
        });
        console.log(m.getSnapshot()?.context, "context")
        return {
          inputRaw: m.getSnapshot()?.context.output as ArthasReq,
        };
      }),
    },
    guards: {
      //   inputValid: (context, event) => {
      //     console.log(context, event)
      //     if (event.type === 'SUBMIT') {
      //       try {
      //         JSON.parse(event.value)
      //         return true
      //       } catch (err) {
      //         return false
      //       }
      //     } else {
      //       return false
      //     }

      //   }
      // 判断命令是否有问题
      cmdSucceeded: (context, event) => {
        if (
          event.type !== "done.invoke.getCommon" &&
          event.type !== "done.invoke.getSession"
        ) {
          return false;
        }
        if (["SCHEDULED", "SUCCEEDED"].includes(event.data.state)) return true;

        return false;
      },
      notSession: (context) => {
        // 为了触发类型计算瞎写的
        // if (event.type !== "SUBMIT") return true;
        // if (typeof context.inputValue.value === "string") return false;
        if (!context) return false;
        if (
          ["join_session", "init_session", "close_session", "interrupt_job"]
            .includes(context.inputValue!.action)
        ) {
          return false;
        }
        return true;
      },
      notReq: (context) => {
        if (context.inputValue) return true;
        return false;
      },
      notString: (context, event) => {
        if (event.type !== "SUBMIT") return true;
        if (typeof event.value !== "string") return true;
        return false;
      },
    },
  });

export default machine;
