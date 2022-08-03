type SessionAction =
  | "join_session"
  | " init_session"
  | "close_session"
  | "interrupt_job";
type ResState = "SCHEDULED" | "SUCCEEDED" | "FAILED" | "REFUSED";

type MergeObj<T extends Record<string, any>, U extends Record<string, any>> =
  T extends T ? {
      [k in (keyof T | keyof U)]: k extends keyof T ? T[k]
        : U[k];
    }
    : never;
type JobId<T extends Record<string, any>> = T extends T
  ? MergeObj<T, Record<"jobId", number>>
  : never;
type SessionId<T extends Record<string, any>> = T extends T
  ? MergeObj<T, { sessionId?: string }>
  : never;
type Command<T extends Record<string, any>> = T extends T
  ? MergeObj<T, { command: string }>
  : never;
type CommonAction<T> = T extends T ? MergeObj<
    T,
    { action: "exec" | "async_exec" | "interrupt_job" | "pull_results" }
  >
  : never;
type unionExclude<T, U> = T extends T ? Exclude<T, U> : T;

// 数值计算
type BuildArray<
  Length extends number,
  Ele = unknown,
  Arr extends unknown[] = [],
> = Arr["length"] extends Length ? Arr
  : BuildArray<Length, Ele, [...Arr, Ele]>;

type Sub1<N extends number> =
  BuildArray<N> extends [arr1: unknown, ...arr2: infer Rest]
    ? Rest["length"]
    : never;

// 命令T 可以添加N个参数
type StringInclude<T extends string, N extends number, P = string> = N extends 0
  ? T
  : T | StringInclude<`${T} ${P}`, Sub1<N>>;
type SessionReq =
  | {
    action: "init_session";
  }
  | SessionId<
    {
      action: "join_session" | "close_session";
    } | {
      action: "pull_results";
      "consumerId": string;
    } | {
      action: "async_exec";
      "command": string;
    } | {
      action: "interrupt_job";
    }
  >;

type CommandReq = CommonAction<
  Command<
    {
      requestId?: string;
      sessionId?: string;
      consumerId?: string;
      command: string;
      execTimeout?: number;
    } | {
      command: "sysenv" | "version" | "sysprop";
    } | {
      command: StringInclude<"vmoption",2>;
    }
  >
>;

type ArthasReq = SessionReq | CommandReq;

type StatusResult = {
  type: "status";
  statusCode: 0;
} | {
  type: "status";
  // 实际上不起效果
  statusCode: number;
  message: string;
};

type InputResult = {
  inputStatus: "ALLOW_INPUT" | "DISABLED" | "ALLOW_INTERRUPT";
  type: never;
};
type VmOption = MergeObj<
Record<"name" | "origin" | "value", string>,
Record<"writeable", boolean>
>
type CommandResult = {
  type: "command";
  state: ResState;
  command: string;
} | {
  type: "version";
  version: string;
} | {
  type: "sysenv";
  env: Record<string, string>;
} | {
  type: "sysprop";
  props: Record<string, string>;
} | {
  type: "vmoption";
  vmOptions: vmOption[];
};

type EnchanceResult = {
  success: boolean;
  effect: Record<"listenerId" | "cost" | "classCount" | "methodCount", number>;
  type: "enhancer";
};

type ArthasResResult = JobId<
  | StatusResult
  | InputResult
  | CommandResult
  | EnchanceResult
>;

type ResBody = Command<
  JobId<{
    "results": ArthasResResult[];
    "timeExpired": boolean;
    "jobStatus": "TERMINATED" | "READY";
  }>
>;

type CommonRes = {
  "state": ResState;
  "sessionId": string;
  "requestId"?: string;
  body: ResBody;
};

type AsyncRes = {
  state: ResState;
  sessionId: string;
  requestId?: string;
  body: Command<
    JobId<{
      jobStatus: "READY" | "TERMINATED";
    }>
  >;
};

type SessionRes = {
  "sessionId": string;
  "consumerId": string;
  "state": Exclude<ResState, "FAILED">;
};

type FailRes = SessionId<{
  message: string;
  state: "FAILED" | "REFUSED";
}>;

type ArthasRes = CommonRes | SessionRes | FailRes | AsyncRes;
