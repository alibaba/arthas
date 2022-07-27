type SessionAction = "join_session" | " init_session" | "close_session"|"interrupt_job"

type SessionReq = {
  action: "init_session"
} | SessionId<{
  action: "join_session" | "close_session",
} | {
  action: "pull_results",
  "consumerId": string
} | {
  action: "async_exec",
  "command": string
} | {
  action: "interrupt_job"
}>

type CommandReq = {
  "action": "exec" | "async_exec" | "interrupt_job" | "pull_results",
  "requestId"?: string,
  "sessionId"?: string,
  "consumerId"?: string,
  "command": string,
  "execTimeout"?: number
}
type ArthasReq = SessionReq | CommandReq
  type ResState = "SCHEDULED" | "SUCCEEDED" | "FAILED" | "REFUSED"

type MergeObj<T extends Record<string, any>, U extends Record<string, any>> = {
  [k in (keyof T | keyof U)]: k extends keyof T
  ? T[k]
  : U[k]
}
type JobId<T extends Record<string, any>> = T extends T ? MergeObj<T, Record<'jobId', number>> : never
type SessionId<T extends Record<string, any>> = T extends T ? MergeObj<T, {sessionId?: string}> : never
type unionExclude<T, U> = T extends T ? Exclude<T,U>:T
type StatusResult = {
  type: "status",
  statusCode: 0
} | {
  type: "status",
  statusCode: Exclude<number, 0>,
  message: string
}

type InputResult = {
  inputStatus: "ALLOW_INPUT" | "DISABLED" | "ALLOW_INTERRUPT",
  type: never
}

type CommandResult = {
  type: "command",
  state: ResState,
  command: string
} | {
  type: "version",
  version: string
}

type EnchanceResult = {
  success: boolean,
  effect: Record<"listenerId" | "cost" | "classCount" | "methodCount", number>,
  type: "enhancer"
}

type ArthasResResult =
  JobId<StatusResult
    | InputResult
    | CommandResult
    | EnchanceResult>

type ResBody = JobId<{
  "results": ArthasResResult[],
  "timeExpired": boolean,
  "command": string,
  "jobStatus": "TERMINATED",
}>
type CommonRes = {
  "state": ResState,
  "sessionId": string,
  "requestId"?: string,
  body: {
    "results": ArthasReqResult[],
    "timeExpired": boolean,
    "command": string,
    "jobStatus": "TERMINATED",
    "jobId": number
  }
}

type SessionRes = {
  "sessionId": string,
  "consumerId": string,
  "state": Exclude<ResState, "FAILED">
}
type FailRes = SessionId<{
  message: string,
  state: "FAILED"
}>
type ArthasRes = CommonRes | SessionRes | FailRes

