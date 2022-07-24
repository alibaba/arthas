import { createMachine, assign } from "xstate";
import { fetchStore } from "@/stores/fetch";
const store = fetchStore();

interface CTX {
  inputVal: ArthasReqBody,
  request?: Request,
  response?: ArthasRes,
  resArr: ArthasResResult[],
  err: string
}
type ET =
  | {
    type: "SUBMIT",
    value: string
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
    type: "INIT",
    context:CTX
  }


const machine =
  /** @xstate-layout N4IgpgJg5mDOIC5QGMD2A7WqA2YB0AlhLgMQDKAqgEICyAkgCqKgAOqsBALgRsyAB6IAjAHYALHiEBmAKxiADADYATIoAcytaLEAaEAE9hQgJx5FQtWMUzjU+cvlq1AX2d60mHPmyoAhhAJ0KBIIDHxAgDdUAGt8DyxcPB9-QKgESNRkX24MAG15AF0+Ng4c9D5BBBl5ITNZMREZRUVjeWMtPUMEIXs8Y0UxLRF5KUtBmVd3DATvPwCgkjAAJyXUJbwWbGyAMzWAWzx4ryS51PT0KKyy-KKkEBKuHnK7yuraxXrG5tb2oU7EZSAvBOZrKERCGzKQbGESuNwgdCoCBwPhHRJEXDFdiPXgvRBiZT-bpSKSSeT2KRfKHGMQTeFo2YpIJY0pPCoAyx4OwNCEyERqeQKCFEnoiMzU8RiGFyRpSSYgBl4WAAV2QyDg8DuDzK7IQ6hkeChgLUUmUFkBLSJrT64KkihEgKU9rUsPp02O218BGwyqWYBZOOeoEq+sNBM0pvNqmMRLN8mBtlEIikYiEVhNrqmnkxWuxOrx3TUIrUeHJZfLFddriAA */
  createMachine({
    context: {
      inputVal: { action: "exec", command: "version" },
      request: undefined,
      response: undefined,
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
          SUBMIT: [
            {
              actions: ["getReq","transformInput"],
              target: "loading",
              cond: "inputValid"
            },
            {
              target: 'idle'
            }
          ],
          INIT: {
            target: "idle",
            actions:"initContext"
          }
        },
      },
      loading: {
        invoke: {
          id: 'getdata',
          src: "requestData",
          onDone: [
            {
              actions: ["transformRes", "renderRes"],
              target: "idle",
            },
          ],
          onError: [
            {
              actions: ["returnErr"],
              target: "idle",
            },
          ],
        },
      },
      // success: {
      //   type: "final",

      // },
      // failure: {
      //   type: "final",
      // },
    },
  }, {
    services: {
      requestData: context => fetch(context.request as Request).then(res => res.json(), err => Promise.reject(err))
    },
    actions: {
      initContext: assign({
        request: (context,event)=>{
          if (event.type !== "INIT") return undefined
          return event.context.request
        }
      }),
      transformInput: assign({
        inputVal: (context, event) => {
          if (event.type !== "SUBMIT") return ''
          return JSON.parse(event.value)
        }
      }),
      getReq:assign({
        request: (context, event)=>store.getRequest(context.inputVal)
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
          return context.resArr.concat(context.response!.body.results)
        },
      }),
      returnErr:
        assign({
          err: (context, event) => {
            if (event.type !== "error.platform") return ''
            return event.data
          }
        })

    },
    guards: {
      inputValid: (context, event) => {
        console.log(context, event)
        if (event.type === 'SUBMIT') {
          try {
            JSON.parse(event.value)
            return true
          } catch (err) {
            return false
          }
        } else {
          return false
        }

      }
    }
  });

export default machine