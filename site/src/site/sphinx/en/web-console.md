Web Console
===========

### Info

Arthas supports Web Console. When being attached successfully, users can access it: [http://localhost:8563/](http://localhost:8563/).

Users can fill in the IP and connect to the remote Arthas as:

![web console](_static/web-console-local.png)

### Basic Authentication 

We can turn to reversed proxy (Nginx) to add basic authentication to ensure security. This can be easily achieved in Unix/Linux/Mac as follows:

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

$ cat /etc/apache2/.htpasswd
user1:$apr1$/woC1jnP$KAh0SsVn5qeSMjTtn0E9Q0

# encoded password stored

# another user, no need to use -c option here again
sudo htpasswd /etc/apache2/.htpasswd user2
```

#### Configure Nginx

```
$ cat /etc/nginx/conf.d/arthas.conf 

# after being authenticated, redirected to the proper address
server {
  listen 8564;

  auth_basic "Administrator Area";
  auth_basic_user_file /etc/apache2/.htpasswd;

  location / {
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

Access `http://127.0.0.1:8564` now. 

If you have suggestions for the Web Console, please leave a message here: [https://github.com/alibaba/arthas/issues/15](https://github.com/alibaba/arthas/issues/15)
