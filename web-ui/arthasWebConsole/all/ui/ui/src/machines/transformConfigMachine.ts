import { assign, createMachine } from "xstate";
import { respond } from "xstate/lib/actions";
type Output = object;
interface CTX {
  inputValue: string;
  output?: Output;
  notJSON: symbol;
  err: string;
}
type ET =
  | {
    type: "INPUT";
    data: string;
  }
  | {
    type: "FAILURE",
    data: string
  }
  | {
    type: "SUCCESS";
    data: Output,
  };

const transformMachine =
  /** @xstate-layout N4IgpgJg5mDOIC5QGMD2A7WqA2YB0AlhLgMQDKAqgEICyAkgCqKgAOqsBALgRsyAB6IAjAHYALHiEBmAKxiADADYATIoAcytaLEAaEAE9hQgJx5FQtWMUzjU+cvlq1AX2d60mHPmyoAhhAJ0KBIIDHxAgDdUAGt8DyxcPB9-QKgESNRkX24MAG15AF0+Ng4c9D5BBBl5ITNZMREZRUVjeWMtPUMEIXs8Y0UxLRF5KUtBmVd3DATvPwCgkjAAJyXUJbwWbGyAMzWAWzx4ryS51PT0KKyy-KKkEBKuHnK7yuraxXrG5tb2oU7EZSAvBOZrKERCGzKQbGESuNwgdCoCBwPhHRJEXDFdiPXgvRBiZT-bpSKSSeT2KRfKHGMQTeFo2YpIJY0pPCoAyx4OwNCEyERqeQKCFEnoiMzU8RiGFyRpSSYgBl4WAAV2QyDg8DuDzK7IQ6hkeChgLUUmUFkBLSJrT64KkihEgKU9rUsPp02O218BGwyqWYBZOOeoEq+sNBM0pvNqmMRLN8mBtlEIikYiEVhNrqmnkxWuxOrx3TUIrUeHJZfLFddriAA */
  createMachine(
    {
      id: "JSON_TO_OBJ",
      schema: {
        context: {} as CTX,
        events: {} as ET,
      },
      context: {
        inputValue: "???",
        notJSON: Symbol(""),
        output: undefined,
        err: "",
      },
      initial: "idle",
      states: {
        idle: {
          on: {
            INPUT: [{
              cond: "isString",
              actions: "getVal",
              target: "handle",
            }, {
              actions: assign(()=>({err:"not string"})) as any,
              target: "failure",
            }],
          },
        },
        handle: {
          always: [
            { cond: "isJSON", actions: "handleEnvJSON", target: "success" },
            {
              actions: assign<CTX,ET>({err:"not JSON"}),
              target: "failure",
            },
          ],
        },
        failure: {
          tags:["result"],
          entry: respond("FAILURE"),
          type: "final",
        },
        success: {
          tags:["result"],
          entry:respond("SUCCESS"),
          type: "final",
        },
      },
    },
    {
      actions: {
        getVal: assign((context, e) => {
          if (e.type !== "INPUT") return {};
          return {
            inputValue: e.data,
          };
        }),
        handleEnvJSON: assign((context) => {
          const output = JSON.parse(context.inputValue);
          return {
            output,
          };
        }),
      },
      guards: {
        isString: (ctx, e) => {
          if (e.type !== "INPUT") return true;
          if (typeof e.data !== "string") return false;
          return true;
        },
        isJSON: (ctx, e) => {
          // if (e.type !== "INPUT") return true;
          try {
            JSON.parse(ctx.inputValue);
            return true;
          } catch {
            return false;
          }
        },
      },
    },
  );

export default transformMachine;
