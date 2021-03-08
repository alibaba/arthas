auth
===

> Authenticates the current session

### Configure username and password

When attaching, you can specify a password on the command line. such as:

```
java -jar arthas-boot.jar --password ppp
```

* The user can be specified by the `--username` option, the default value is `arthas`.
* You can also configure username/password in `arthas.properties`. The priority of the command line is higher than that of the configuration file.
* If only `username` is configured and no `password` is configured, a random password will be generated and printed in `~/logs/arthas/arthas.log`

  ```
  Using generated security password: 0vUBJpRIppkKuZ7dYzYqOKtranj4unGh
  ```

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

#### HTTP Authorization Header(recommended)

Arthas uses the HTTP standard Basic Authorization.

* Reference: [https://developer.mozilla.org/en-US/docs/Web/HTTP/Authentication](https://developer.mozilla.org/en-US/docs/Web/HTTP/Authentication)

For example, if the user name is: `admin` and the password is `admin`, the combination is a string: `admin:admin`, the base64 result is: `YWRtaW46YWRtaW4=`, then the HTTP request adds the `Authorization` header:

```bash
curl 'http://localhost:8563/api' \
  -H 'Authorization: Basic YWRtaW46YWRtaW4=' \
  --data-raw '{"action":"exec","command":"version"}' 
```

#### URL parameters

It supports passing username and password in parameters. such as:

```bash
curl 'http://localhost:8563/api?password=admin' \
  --data-raw '{"action":"exec","command":"version"}' 
```