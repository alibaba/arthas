Web Console
===========

### You should know

Arthas supports Web Console. When being attached successfully, users can access it at: [http://localhost:8563/](http://localhost:8563/).

Users can fill up the IP and port to connect to the remote Arthas as:

![web console](_static/web-console-local.png)

### Basic Authentication 

We can turn to reversed proxy (Nginx) to add basic authentication to ensure security. This can be easily achieved in Unix/Linux/Mac. Take Ubuntu 16.04 as an example:

#### Install apache2-utils

```
sudo apt-get install apache2 apache2-doc apache2-utils

# if it's necessary, update and upgrade repositories
sudo apt-get update && sudo apt-get upgrade
```

#### Create some accounts

```
sudo htpasswd -c /etc/apache2/.htpasswd user1
# you will be prompted to enter the password here

# encoded password stored
$ cat /etc/apache2/.htpasswd
user1:$apr1$/woC1jnP$KAh0SsVn5qeSMjTtn0E9Q0

# add another user, no need to use -c option here again
sudo htpasswd /etc/apache2/.htpasswd user2
```

#### Configure Nginx

```
$ cat /etc/nginx/conf.d/arthas.conf 

# after being authenticated, redirected to the proper address
server {
  # testing in the same host, use another port to listen;
  listen 8564;

  auth_basic "Administrator Area";
  auth_basic_user_file /etc/apache2/.htpasswd;

  location / {
    # after being authenticated, redirect the request to arthas at port: 8563;
    proxy_pass http://localhost:8563/;
  }
}
```

Remember to configure `/etc/nginx/nginx.conf` to include it as

```
$ cat /etc/nginx/nginx.conf 

# ...

http {
    # to include the newly added configuration;
    include /etc/nginx/conf.d/*.conf;
    # ...
}
```

Restart Nginx: `sudo service nginx restart`; if there is anything wrong, please check `tail -f -n 100 /var/log/nginx/error.log`.

### Test

```

# start Arthas

$ bin/as.sh
```

Access [http://localhost:8564](http://localhost:8564) now. 


If you have suggestions for the Web Console, please leave a message here: [https://github.com/alibaba/arthas/issues/15](https://github.com/alibaba/arthas/issues/15)
