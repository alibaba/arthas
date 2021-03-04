auth
===

> Authenticates the current session

### Configure username and password

When attaching, you can specify a password on the command line. such as:

```
java -jar arthas-boot.jar --password ppp
```

* The user can be specified by the `--username` option, the default value is `arthas`.
* You can also configure username/password in arthas.properties. The priority of the command line is higher than that of the configuration file.

### Authenticate in the telnet console

After connecting to arthas, directly executing the command will prompt for authentication:

```bash
[arthas@37430]$ help
Error! command not permitted, try to use 'auth' command to authenticates.
```

Use the `auth` command to authenticate, and you can execute other commands after success.

```
[arthas@37430]$ auth ppp
Authentication result: true
```

* The user can be specified by the `--username` option, the default value is `arthas`.

### Web console Authentication

Open the browser, there will be a pop-up window prompting you to enter your username and password.

After success, you can directly connect to the web console.

### HTTP API Authentication

Arthas uses the HTTP standard Basic Authorization.

* Reference: [https://developer.mozilla.org/en-US/docs/Web/HTTP/Authentication](https://developer.mozilla.org/en-US/docs/Web/HTTP/Authentication)