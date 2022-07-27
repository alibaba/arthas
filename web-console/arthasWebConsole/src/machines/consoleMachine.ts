import { createMachine, assign } from "xstate";
import { fetchStore } from "@/stores/fetch";
import { publicStore } from "@/stores/public";
import { Pinia } from "pinia";
// const store = fetchStore();

interface CTX {
  inputVal: unionExclude<ArthasReq,'sessionId'>,
  request?: Request,
  response?: ArthasRes,
  resArr: ArthasResResult[],
  err: string,
  publicStore:any,
  fetchStore: any
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
    type: "done.invoke.getdata",
    data: ArthasRes
  }
  | {
    type: "INIT"
  }


const machine =
  /** @xstate-layout N4IgpgJg5mDOIC5QGMD2A7WqA2YB0AlhLgMQDKAqgEICyAkgCqKgAOqsBALgRsyAB6IAjAHYALHiEBmAKxiADADYATIoAcytaLEAaEAE9hQgJx5FQtWMUzjU+cvlq1AX2d60mHPmyoAhhAJ0KBIIDHxAgDdUAGt8DyxcPB9-QKgESNRkX24MAG15AF0+Ng4c9D5BBBl5ITNZMREZRUVjeWMtPUMEIXs8Y0UxLRF5KUtBmVd3DATvPwCgkjAAJyXUJbwWbGyAMzWAWzx4ryS51PT0KKyy-KKkEBKuHnK7yuraxXrG5tb2oU7EZSAvBOZrKERCGzKQbGESuNwgdCoCBwPhHRJEXDFdiPXgvRBiZT-bpSKSSeT2KRfKHGMQTeFo2YpIJY0pPCoAyx4OwNCEyERqeQKCFEnoiMzU8RiGFyRpSSYgBl4WAAV2QyDg8DuDzK7IQ6hkeChgLUUmUFkBLSJrT64KkihEgKU9rUsPp02O218BGwyqWYBZOOeoEq+sNBM0pvNqmMRLN8mBtlEIikYiEVhNrqmnkxWuxOrx3TUIrUeHJZfLFddriAA */
  createMachine({
    context: {
      inputVal: { action: "exec", command: "version" },
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
          // SUBMIT: [
          //   {
          //     actions: ["storageReq","getReq"],
          //     target: "loading"
          //   }
          // ],
          INIT: {
            target: "ready",
            actions:assign<CTX,ET>({
              publicStore: () => publicStore(),
              fetchStore: () => fetchStore()
            }) as any
          }
        },
      },
      ready: {
        on:{
          SUBMIT: [
            { cond: "isSession",
              actions: ["storageCommonReq","getReq"],
              target: "loading"
            },{
              actions: ["storageSessionReq","getReq"],
              target: "session"
            }
          ],
        }
      },
      loading: {
        invoke: {
          id: 'getdata',
          src: "requestData",
          onDone: [
            {
              cond: 'cmdSucceeded',
              actions: ["transformRes", "renderRes"],
              target: "ready",
            },{
              actions: "returnErr",
              target: "ready"
            }
          ],
          onError: [
            {
              actions: ["returnErr"],
              target: "ready",
            },
          ],
        },
      },
      session:{
        invoke: {
          id: 'getdata',
          src: "requestData",
          onDone: [
            {
              cond: 'cmdSucceeded',
              actions: ["transformRes", "renderRes"],
              target: "ready",
            },{
              actions: "returnErr",
              target: "ready"
            }
          ],
          onError: [
            {
              actions: ["returnErr"],
              target: "ready",
            },
          ],
        },
      },
      success: {
        type: "final",
        always:"ready"
      },
      failure: {
        type: "final",
        always:"ready"
      },
    },
  }, {
    services: {
      requestData: context => fetch(context.request as Request).then(res => res.json(), err => Promise.reject(err))
    },
    actions: {
      // initStore: assign({
      //   // request: (context,event)=>{
      //   //   if (event.type !== "INIT") return undefined
      //   //   return event.context.request
      //   // },
      //   publicStore: () => publicStore(),
      //   fetchStore: () => fetchStore()
      // }),
      // transformInput: assign({
      //   inputVal: (context, event) => {
      //     if (event.type !== "SUBMIT") return ''
      //     return JSON.parse(event.value)
      //   }
      // }),
      storageCommonReq:assign({
        inputVal:(context,event)=>{
          if (event.type !== "SUBMIT") return {} as CommandReq
          return event.value
        }
      }),
      storageSessionReq:assign({
        inputVal:(context,event)=>{
          if (event.type !== "SUBMIT") return {} as SessionReq
          return event.value
        }
      }),
      getReq:assign({
        request: (context, event)=>context.fetchStore.getRequest(context.inputVal)
      }),
      transformRes: assign({
        response: (context, event) => {
          if (event.type !== "done.invoke.getdata") return undefined
          return event.data
        }
      }),
      renderRes: assign({
        resArr: (context) => {
          console.log(context.resArr,context.response)
          if (context.response !== undefined && Object.hasOwn(context.response, 'body')){
            return context.resArr.concat((context.response as CommonRes).body.results)
          }
          // 错误格式不统一，有点难受
          console.log(context.response)
          return context.resArr
        },
      }),
      returnErr:
        assign({
          err: (context, event) => {
            if (event.type !== "error.platform" && event.type !== "done.invoke.getdata") return ''
            context.publicStore.$patch({isErr:true, ErrMessage:event.data})
            console.log(event.data,context.publicStore)
            return event.data
          }
        })

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
      cmdSucceeded: (context, event)=>{
        if (event.type !== "done.invoke.getdata") return false
        if (event.data.state === "SUCCEEDED") return true

        return false
      },
      isSession: (context,event)=>{
        if(event.type !=="SUBMIT") return false
        if(["join_session"," init_session","close_session","interrupt_job"].includes(event.value.action)) return false
        return true
      }
    }
  });

export default machine