
type SessionAction = "join_session" | " init_session" | "close_session"

type ArthasReqBody = {
  "action": "exec" | "async_exec" | "interrupt_job" | "pull_results" | SessionAction,
  "requestId"?: string,
  "sessionId"?: string,
  "consumerId"?: string,
  "command": string,
  "execTimeout"?: number
}

type ResState = "SCHEDULED" | "SUCCEEDED" | "FAILED" | "REFUSED"

type ResResultBase = {
  "jobId": number,
  "statusCode": number,
  type: string
}

type StatusResult = {
  "type": "status"
}

type InputResult = {
  inputStatus: "ALLOW_INPUT" | "DISABLED" | "ALLOW_INTERRUPT"
}

type CommandResult = {
  "type": "command",
  "state": ResState,
  "command": string
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

type ArthasResResult = (StatusResult | InputResult | CommandResult | EnchanceResult) & ResResultBase

type ArthasRes = {
  "state": ResState,
  "sessionId": string,
  "requestId": string,
  "body": {
    "results": ArthasReqResult[],
    "timeExpired": boolean,
    "command": string,
    "jobStatus": string,
    "jobId": number
  }
}