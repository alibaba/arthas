
When the job is executing in the foreground, for example, directly executing the command` trace Test t`, or executing the background job command `trace Test t &`, then putting the job back to the foreground via fg command, the console cannot continue to execute other command, but can receive and process the following keyboard events:

`ctrl + z`: Suspends the job, the job status will change to Stopped, and the job can be restarted by `bg <job-id>` or `fg <job-id>`

`ctrl + c`: Stops the job

`ctrl + d`: According to linux semantics this should lead to exit the terminal, right now Arthas has not implemented this yet, therefore simply ignore this keystroke.
