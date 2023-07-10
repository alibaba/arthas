
* When a job is executed in the background or in suspended status (use `ctrl + z` to suspend job), `fg <job-id>` can transfer the job to the foreground to continue to run.

* When a job is in suspended status (use `ctrl + z` to suspend job), `bg <job-id>` can put the job to the background to continue to run.

* A job created by other session can only be put to the foreground to run by using `fg` in the current session.
