
type SessionAction = "join_session" | " init_session" | "close_session"

type SessionReq = {
  action: "init_session"
} | {
  action: "join_session" | "close_session",
  "sessionId": string
} | {
  action: "pull_results",
  "sessionId": string,
  "consumerId": string
} | {
  action: "async_exec",
  "command": string,
  "sessionId": string
} | {
  action: "interrupt_job",
  "sessionId": string
}

type ArthasReqBody = {
  "action": "exec" | "async_exec" | "interrupt_job" | "pull_results" | SessionAction,
  "requestId"?: string,
  "sessionId"?: string,
  "consumerId"?: string,
  "command": string,
  "execTimeout"?: number
}

type ResState = "SCHEDULED" | "SUCCEEDED" | "FAILED" | "REFUSED"

type JobId<T extends object> = {
  [k in keyof T]: T[k]
  "jobId": number
}
type SessionId<T extends object>={
  [k in keyof T]?: T[k]
  sessionId: string
}
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
  "type": "command",
  "state": ResState,
  "command": string
} | {
  type: "version",
  version: string
}

type EnchanceResult = {
  "success": boolean,
  "effect": {
    "listenerId": number,
    "cost": number,
    "classCount": number,
    "methodCount": number
  },
  "type": "enhancer"
}

type ArthasResResult = JobId<StatusResult | InputResult | CommandResult | EnchanceResult>

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
  "state": Exclude<ResState,"FAILED">
}
type FailRes = SessionId<{
	message: string,
	state: "FAILED"
}>
type ArthasRes = CommonRes | SessionRes | FailRes

