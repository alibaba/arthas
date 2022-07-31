import { createMachine, assign } from "xstate";
import { publicStore } from "@/stores/public";
type Output = object
interface CTX {
  inputValue:string,
  output?: Output,
  notJSON:symbol,
  err: string,
  // 暂时先anyscript
  publicStore?: any,
}
type ET =
  | {
    type: "INPUT",
    data: string
  }
  | {
    type: "INIT",
  }
  | {
    type: "TRANSFORM"
  }



const machine =
  /** @xstate-layout N4IgpgJg5mDOIC5QGMD2A7WqA2YB0AlhLgMQDKAqgEICyAkgCqKgAOqsBALgRsyAB6IAjAHYALHiEBmAKxiADADYATIoAcytaLEAaEAE9hQgJx5FQtWMUzjU+cvlq1AX2d60mHPmyoAhhAJ0KBIIDHxAgDdUAGt8DyxcPB9-QKgESNRkX24MAG15AF0+Ng4c9D5BBBl5ITNZMREZRUVjeWMtPUMEIXs8Y0UxLRF5KUtBmVd3DATvPwCgkjAAJyXUJbwWbGyAMzWAWzx4ryS51PT0KKyy-KKkEBKuHnK7yuraxXrG5tb2oU7EZSAvBOZrKERCGzKQbGESuNwgdCoCBwPhHRJEXDFdiPXgvRBiZT-bpSKSSeT2KRfKHGMQTeFo2YpIJY0pPCoAyx4OwNCEyERqeQKCFEnoiMzU8RiGFyRpSSYgBl4WAAV2QyDg8DuDzK7IQ6hkeChgLUUmUFkBLSJrT64KkihEgKU9rUsPp02O218BGwyqWYBZOOeoEq+sNBM0pvNqmMRLN8mBtlEIikYiEVhNrqmnkxWuxOrx3TUIrUeHJZfLFddriAA */
  createMachine({
    id:"JSON_TO_OBJ",
    schema:{
      context:{} as CTX,
      events:{} as ET
    },
    context:{
      inputValue:'???',
      notJSON:Symbol(''),
      output:undefined,
      err:"",
      publicStore:undefined
    },
    initial:"idle",
    states:{
      idle:{
        on: {
          INIT:{
            actions:[
              "initStore",
            ],
            target:"ready"
          }
        }
      },
      ready:{
        on: {
          INPUT:[{
            cond:"isString",
            actions:"getVal",
            target:"handle"
          },{
            target:"failure"
          }]
        }
      },
      handle:{
        on:{
          TRANSFORM:[
            { cond:"isJSON",
              actions:"handleEnvJSON",
              target:"success"
            },{
              target:"failure"
            }
          ]
        }
      },
      failure:{
        entry:"returnErr",
        type:"final"
      },
      success:{
        type: "final"
      }
    }
  },{
    actions:{
      initStore:assign((context,evnet)=>{
        if (evnet.type !== 'INIT') return {}
        return {
          publicStore: publicStore()
        }
      }),
      getVal:assign((context,e)=>{
        if (e.type !=="INPUT") return {}
        return {
          inputValue:e.data
        }
      }),
      handleEnvJSON:assign((context,e)=>{
        if (e.type !=="INPUT") return {}
          const output = JSON.parse(e.data)
        return {
          output
        }
      }),
      returnErr: assign((context, event) => {
          if(event.type === "INPUT"){
            context.publicStore?.$patch({ isErr: true, ErrMessage: "not JSON " })
            return {err:"notJSON"}
          }
          return {}
        
      }),
    },
    guards:{
      isString:(ctx,e)=>{
        if(e.type !== "INPUT") return true
        if(typeof e.data !== "string") return false
        return true
      },
      isJSON:(ctx,e)=>{
        if (e.type !== "INPUT") return true
        try {
          JSON.parse(e.data)
          return true
        } catch {
          return false
        }
      }
    }
  });

export default machine