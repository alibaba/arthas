type SessionAction =
  | "join_session"
  | "init_session"
  | "close_session";
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
// type Command<T extends Record<string|"results", any>> = T extends T
//   ?  T["results"][0] extends JobId<CommandResult>
//     ? "command" extends keyof T["results"][0]
//       ? MergeObj<T, {command: T["results"][0]["command"]}>
//       : never
//     : MergeObj<T, { command: never }>
//   : never;
type Command<T extends Record<string | "results", any>> = T extends T
  ? MergeObj<T, { command: string }>
  : never;

type CommonAction<T> = T extends T ? MergeObj<
    T,
    { action: "exec" }
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
type StringInclude<T extends string, N extends number, P = string> = T extends T
  ? N extends 0 ? T
  : T | StringInclude<`${T} ${P}`, Sub1<N>>
  : never;

type SessionReq =
  | {
    action: "init_session";
  }
  | SessionId<{
    action: "join_session" | "close_session";
  }>;

type AsyncReq = SessionId<
  | {
    action: "interrupt_job";
  }
  | MergeObj<
    {
      action: "async_exec";
    },
    {
      command: "dashboard";
    } | {
      command: StringInclude<"stack", 3>;
    }
  >
>;
type PullResults = SessionId<{
  action: "pull_results";
  consumerId?: string;
}>;
type CommandReq = CommonAction<
  {
    command:
      | "sysenv"
      | "version"
      | "sysprop"
      | "pwd"
      | "jvm"
      | "memory"
      | "perfcounter -d"
      | "classloader -a"
      | "classloader --url-stat"
      | `sm -d ${string}`
      | `sm ${string}`
      | `jad ${string}`
      | `dump ${string}`
      | "retransform -l"
      | `retransform ${string}`
      | `retransform --classPattern ${string}`
      | `mbean`
      | `mbean ${string}`
      | `mbean -m ${string}`
      | "vmtool --action forceGc";
  } | {
    command: StringInclude<"vmoption" | "thread", 2>;
  } | {
    command: StringInclude<"sc", 3>;
  } | {
    command: StringInclude<"heapdump" | "heapdump --live" | "reset", 1>;
  }
>;

type ArthasReq = SessionReq | CommandReq | AsyncReq | PullResults;

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
type ThreadInfo = {
  "blockedCount": 876;
  "blockedTime": -1;
  "inNative": true;
  "lockOwnerId": -1;
  "lockedMonitors": [];
  "lockedSynchronizers": [];
  "stackTrace": {
    "className": string;
    "fileName": string;
    "lineNumber": number;
    "methodName": number;
    "nativeMethod": boolean;
  }[];
  "suspended": boolean;
  "threadId": number;
  "threadName": string;
  "threadState": "WAITING" | "TIMED_WAITING" | "RUNNABLE";
  "waitedCount": number;
  "waitedTime": number;
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
type MemoryInfo = Record<"heap" | "nonheap" | "buffer_pool", {
  "max": number;
  "name": string;
  "total": number;
  "type": string;
  "used": number;
  "usage"?: number;
}[]>;
type RuntimeInfo = {
  "javaHome": string;
  "javaVersion": string;
  "osName": string;
  "osVersion": string;
  "processors": number;
  "systemLoadAverage": number;
  "timestamp": number;
  "uptime": number;
};
type ClassDetailInfo = {
  "annotation": boolean;
  "annotations": string[];
  "anonymousClass": boolean;
  "array": boolean;
  "classInfo": string;
  "classLoaderHash": string;
  "classloader": string[];
  "codeSource": string;
  "enum": string;
  "interface": string;
  "interfaces": string[];
  "localClass": string;
  "memberClass": string;
  "modifier": string;
  "name": string;
  "primitive": boolean;
  "simpleName": string;
  "superClass": string[];
  "synthetic": boolean;
};
type ClassField = {
  annotations: string[];
  "modifier": string;
  "name": string;
  "static": boolean;
  "type": string;
  "value": any;
};
type MethodInfo = {
  "classLoaderHash": string;
  "constructor": boolean;
  "declaringClass": string;
  "descriptor": string;
  "exceptions": string[];
  "parameters": string[];
  "annotations": string[];
  "methodName": string;
  "modifier": string;
  "returnType": string;
};
type ClassInfo = MergeObj<ClassDetailInfo, { fields: ClassField[] }>;

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
  threadInfo: never;
  busyThreads: never;
  type: "thread";
} | {
  threadInfo: ThreadInfo;
  threadStats: never;
  busyThreads: never;
  type: "thread";
} | {
  all: boolean;
  busyThreads: BusyThread[];
  threadInfo: never;
  threadStats: never;
  type: "thread";
} | {
  "jvmInfo": JvmInfo;
  type: "jvm";
} | {
  memoryInfo: MemoryInfo;
  type: "memory";
} | {
  perfCounters: {
    "name": string;
    "units": string;
    "value": string | number;
    "variability": string;
  }[];
  type: "perfcounter";
} | {
  classSet: {
    classes: string[];
    classloader: Record<"hash" | "name" | "parent", string>;
    segment: number;
  };
  urlStats: never;
  type: "classloader";
} | {
  type: "classloader";
  urlStats: {
    [x: `{"hash":${string},"name":${string}}`]: {
      unUsedUrls: string[];
      usedUrls: string[];
    };
  };
  command: "classloader --url-stat";
  classSet: never;
} | {
  "classInfo": ClassInfo;
  detailed: true;
  type: "sc";
  segment: 0;
  withField: true;
} | {
  classNames: string[];
  detailed: false;
  segment: number;
  type: "sc";
  withField: false;
} | {
  "classInfo": ClassDetailInfo;
  "detailed": true;
  "jobId": 31365;
  "segment": 0;
  "type": "sc";
  "withField": false;
} | {
  "detail": true;
  "methodInfo": MethodInfo;
  "type": "sm";
} | {
  "classInfo": {
    "classLoaderHash": string;
    "classloader": string[];
    "name": string;
  };
  "location": string;
  "mappings": Record<string, number>;
  "source": string;
  "type": "jad";
} | {
  retransformCount: number;
  retransformEntries: {
    bytes: string;
    className: string;
    id: number;
    transformCount: number;
  }[];
  retransformClasses: never;
  type: "retransform";
} | {
  retransformCount: number;
  retransformEntries: never;
  retransformClasses: string[];
  type: "retransform";
} | {
  "dumpedClasses": {
    "classLoaderHash": string;
    "classloader": string[];
    "location": string;
    "name": string;
  }[];
  "type": "dump";
} | {
  "gcInfos": {
    "collectionCount": number;
    "collectionTime": number;
    "name": string;
  }[];
  "memoryInfo": MemoryInfo;
  "runtimeInfo": RuntimeInfo;
  "threads": ThreadStats[];
  "type": "dashboard";
} | {
  mbeanNames: string[];
  mbeanMetadata: never;
  mbeanAttribute: never;
  type: "mbean";
} | {
  mbeanNames: never;
  mbeanMetadata: {
    [x: string]: {
      "attributes": {
        description: string;
        is: boolean;
        name: string;
        readable: boolean;
        type: string;
        writable: boolean;
        openType: Record<string, string>;
      }[];
      "className": string;
      "constructors": {
        "description": string;
        "name": string;
        "signature": {
          "description": string;
          "name": string;
          "type": string;
        }[];
      }[];
      "description": string;
      "notifications": any[];
      "operations": {
        "description": string;
        "impact": number;
        "name": string;
        "returnType": string;
        "signature": {
          "description": string;
          "name": string;
          "type": string;
        }[];
      }[];
    };
  };
  mbeanAttribute: never;
  type: "mbean";
} | {
  mbeanNames: never;
  mbeanMetadata: never;
  mbeanAttribute: {
    [x: string]: {
      name: string;
      value: string | number | boolean | (number[]);
    }[];
  };
  type: "mbean";
} | {
  "dumpFile": string;
  "live": boolean;
  "type": "heapdump";
} | {
  "type": "vmtool";
  "value": string[];
} | {
  "affect": {
    "classCount": number;
    "cost": number;
    "listenerId": number;
    "methodCount": number;
  };
  "type": "reset";
} | {
  "classloader": string;
  "cost": number;
  "daemon": boolean;
  "priority": number;
  "stackTrace": {
    "className": string;
    "fileName": string;
    "lineNumber": number;
    "methodName": string;
    "nativeMethod": boolean;
  }[];
  "threadId": string;
  "threadName": string;
  // date clock
  "ts": `${string} ${string}`;
  "type": "stack";
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
  // results:never;
  body: Command<
    JobId<{
      jobStatus: "READY" | "TERMINATED";
    }>
  >;
};

type SessionRes = {
  "sessionId": string;
  "consumerId": string;
  body: never;
  "state": Exclude<ResState, "FAILED">;
};

type FailRes = SessionId<{
  message: string;
  state: "FAILED" | "REFUSED";
  body: never;
}>;

type ArthasRes = CommonRes | SessionRes | FailRes | AsyncRes;

// autoComplete
type Item = { name: string; value: any };
