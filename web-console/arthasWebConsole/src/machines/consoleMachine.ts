import { createMachine, assign } from "xstate";
import { fetchStore } from "@/stores/fetch";
import { publicStore } from "@/stores/public";

interface CTX {
  request?: Request,
  response?: ArthasRes,
  resArr: (ArthasResResult | SessionRes|AsyncRes)[],
  err: string,
  // 暂时先anyscript
  publicStore?: any,
  fetchStore?: any
}
type ET =
  | {
    type: "SUBMIT",
    value: ArthasReq
  }
  | {
    type: "error.platform",
    data: any
  }
  | {
    type: "done.invoke.getCommon",
    data: CommonRes
  }
  | {
    type: "done.invoke.getSession",
    data: SessionRes
  }
  | {
    type: "INIT"
  }
  | {
    type: "CLEAR_RESARR"
  }


const machine =
  /** @xstate-layout N4IgpgJg5mDOIC5QGMD2A7WqA2YB0AlhLgMQDKAqgEICyAkgCqKgAOqsBALgRsyAB6IAjAHYALHiEBmAKxiADADYATIoAcytaLEAaEAE9hQgJx5FQtWMUzjU+cvlq1AX2d60mHPmyoAhhAJ0KBIIDHxAgDdUAGt8DyxcPB9-QKgESNRkX24MAG15AF0+Ng4c9D5BBBl5ITNZMREZRUVjeWMtPUMEIXs8Y0UxLRF5KUtBmVd3DATvPwCgkjAAJyXUJbwWbGyAMzWAWzx4ryS51PT0KKyy-KKkEBKuHnK7yuraxXrG5tb2oU7EZSAvBOZrKERCGzKQbGESuNwgdCoCBwPhHRJEXDFdiPXgvRBiZT-bpSKSSeT2KRfKHGMQTeFo2YpIJY0pPCoAyx4OwNCEyERqeQKCFEnoiMzU8RiGFyRpSSYgBl4WAAV2QyDg8DuDzK7IQ6hkeChgLUUmUFkBLSJrT64KkihEgKU9rUsPp02O218BGwyqWYBZOOeoEq+sNBM0pvNqmMRLN8mBtlEIikYiEVhNrqmnkxWuxOrx3TUIrUeHJZfLFddriAA */
  createMachine({
    context: {
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
      }
    },
    id: "console",
    initial: "idle",
    states: {
      idle: {
        on: {
          // 延迟pinia的挂载时机
          INIT: {
            target: "ready",
            actions: "initStore"
          }
        },
      },
      ready: {
        on: {
          SUBMIT: [
            {
              cond: "notSession",
              // actions: ["storageCommonReq"],
              target: "common"
            }, {
              // actions: ["storageSessionReq"],
              target: "session"
            }
          ],
          CLEAR_RESARR: { target: "ready", actions: "clearResArr" }
        },
        exit: ["getReq"]
      },
      common: {
        tags: ["loading"],
        invoke: {
          id: 'getCommon',
          src: "requestData",
          onDone: [
            {
              cond: 'cmdSucceeded',
              actions: ["transformRes"],
              target: "success",
            }, {
              target: "failure"
            }
          ],
          onError: [
            {
              target: "failure",
            },
          ],
        },
      },
      session: {
        tags: ["loading"],
        invoke: {
          id: 'getSession',
          src: "requestData",
          onDone: [
            {
              cond: 'cmdSucceeded',
              actions: ["transformSessionRes"],
              target: "success",
            }, {
              target: "failure"
            }
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
        always: "ready"
      },
      failure: {
        entry: "returnErr",
        always: "ready"
      },

    },
  }, {
    services: {
      requestData: context => fetch(context.request as Request).then(res => res.json(), err => Promise.reject(err))
    },
    actions: {
      initStore: assign((context, event) => {
        if (event.type !== 'INIT') return {}
        return {
          publicStore: publicStore(),
          fetchStore: fetchStore()
        }
      }),
      getReq: assign({
        request: (context, event) => {
          console.log("getReq exit", event, context)
          
          if (event.type !== "SUBMIT" || !context.fetchStore ||!("getRequest" in context.fetchStore)) return new Request('')
          return context.fetchStore?.getRequest(event.value)
        }
      }),
      transformRes: assign({
        response: (context, event) => {
          if (event.type !== "done.invoke.getCommon") return undefined
          return event.data
        }
      }),
      transformSessionRes: assign((context, event) => {
        if (event.type !== "done.invoke.getSession") return {}

        context.fetchStore?.$patch({
          sessionId: event.data.sessionId,
          consumerId: event.data.consumerId
        })
        console.log(context.fetchStore)
        return {
          response: event.data
        }
      }),
      renderRes: assign((context, event) => {
        let resArr:(ArthasResResult|SessionRes|AsyncRes)[] = context.resArr
        const response = context.response
        if (!response) {
          return {}
        }
        console.log(response, "renderRes",context.resArr)
        if (Object.hasOwn(response, 'body') && Object.hasOwn((response as CommonRes).body, "results")) {
          // 估计是ts的问题
          resArr = resArr.concat((context.response as CommonRes).body.results)
        } else {
          resArr = resArr.concat([response] as (SessionRes|AsyncRes)[])
        }
        return { resArr: resArr.filter(v=>v&&!Number.isNaN(v)) }
      }),
      returnErr: assign({
        err: (context, event) => {
          if (event.type !== "error.platform"
            && event.type !== "done.invoke.getCommon"
            && event.type !== "done.invoke.getSession") return ''
          context.publicStore?.$patch({ isErr: true, ErrMessage: event.data })
          return event.data
        }
      }),
      clearResArr: assign((context, event) => ({ resArr: [] }))
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
        if (event.type !== "done.invoke.getCommon"
          && event.type !== "done.invoke.getSession"
        ) return false
        if (["SCHEDULED", "SUCCEEDED"].includes(event.data.state)) return true

        return false
      },
      notSession: (context, event) => {
        // 为了触发类型计算瞎写的
        console.log(event, "qwer")
        if (event.type !== "SUBMIT") return false
        console.log(event.value.action,["join_session", "init_session", "close_session", "interrupt_job"].indexOf("init_session"))
        if (["join_session", "init_session", "close_session", "interrupt_job"].includes(event.value.action)) return false
        console.log('123')
        return true
      }
    }
  });

export default machine