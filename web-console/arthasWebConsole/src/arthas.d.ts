type SessionAction =
  | "join_session"
  | "init_session"
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

type Sub1<N extends number> = BuildArray<N> extends
  [arr1: unknown, ...arr2: infer Rest] ? Rest["length"]
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
      consumerId: string;
    } | {
      action: "async_exec";
      command: string;
    } | {
      action: "interrupt_job";
    }
  >;

type CommandReq = CommonAction<
  // {
  //   requestId?: string;
  //   sessionId?: string;
  //   consumerId?: string;
  //   command: string;
  //   execTimeout?: number;
  // } |
  {
    command:
      | "sysenv"
      | "version"
      | "sysprop"
      | "pwd"
      | "jvm"
      | "memory"
      | "perfcounter";
  } | {
    command: StringInclude<"vmoption", 2>;
  } | {
    command: StringInclude<"thread", 2>;
  }
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
>;
type ThreadStats = {
  "cpu": number;
  "daemon": boolean;
  "deltaTime": number;
  "group": "system";
  "id": number;
  "interrupted": boolean;
  "name": string;
  "priority": number;
  "state": "WAITING" | "TIMED_WAITING" | "RUNNABLE";
  "time": number;
};
type BusyThread = {
  "blockedCount": number;
  "blockedTime": number;
  "cpu": number;
  "daemon": true;
  "deltaTime": number;
  "group": string;
  "id": number;
  "inNative": boolean;
  "interrupted": boolean;
  "lockInfo": {
    "className": string;
    "identityHashCode": boolean;
  };
  "lockName": string;
  "lockOwnerId": number;
  "lockedMonitors": any[];
  "lockedSynchronizers": any[];
  "name": string;
  "priority": 10;
  "stackTrace": {
    "className": string;
    "fileName": string;
    "lineNumber": number;
    "methodName": string;
    "nativeMethod": boolean;
  }[];
  "state": "WAITING" | "TIMED_WAITING" | "RUNNABLE";
  "suspended": string;
  "time": number;
  "waitedCount": number;
  "waitedTime": number;
};
type JvmInfo = {
  "RUNTIME": Record<"name" | "value", string>[];
  "CLASS-LOADING": { name: string; value: number | boolean }[];
  "COMPILATION": { name: string; value: number | string; desc: string }[];
  "GARBAGE-COLLECTORS": {
    name: string;
    value: { name: string; collectionCount: number; collectionTime: number };
    desc: string;
  }[];
  "MEMORY-MANAGERS": { name: string; value: string[] }[];
  "MEMORY": {
    "desc": string;
    "name": string;
    "value": {
      "name": string;
      "init": number;
      "used": number;
      "committed": number;
      "max": number;
    } | number;
  }[];
  "OPERATING-SYSTEM": Record<"name" | "value", string>[];
  "THREAD": {
    "name": string;
    "value": number;
  }[];
  "FILE-DESCRIPTOR": {
    "name": string;
    "value": number;
  }[];
};
type MemoryInfo = Record<string, {
  "max": number;
  "name": string;
  "total": number;
  "type": string;
  "used": number;
}[]>;
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
} | {
  type: "pwd";
  workingDir: string;
} | {
  all: boolean;
  threadStateCount: Record<
    | "NEW"
    | "RUNNABLE"
    | "BLOCKED"
    | "WAITING"
    | "TIMED_WAITING"
    | "TERMINATED",
    number
  >;
  threadStats: ThreadStats[];
  type: "thread";
} | {
  all: boolean;
  busyThreads: BusyThread[];
  type: "thread";
} | {
  "jvmInfo": JvmInfo;
  type: "jvm";
} | {
  memoryInfo: MemoryInfo;
  type: "memory";
} | {
  perfCounters: { name: string; value: string | number }[];
  type: "perfcounter";
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
