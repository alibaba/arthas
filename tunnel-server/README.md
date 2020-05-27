## How it works

Tunnel server/client use websocket protocol.

For example:

1. Arthas tunnel server listen at `192.168.1.10:7777`

2. Arthas tunnel client register to the tunnel server

   tunnel client connect to tunnel server with URL: `ws://192.168.1.10:7777/ws?method=agentRegister`

   tunnel server response a text frame message: `response:/?method=agentRegister&id=bvDOe8XbTM2pQWjF4cfw`

   This connection is `control connection`.

3. The browser try connect to remote arthas agent

   start connect to tunnel server with URL: `'ws://192.168.1.10:7777/ws?method=connectArthas&id=bvDOe8XbTM2pQWjF4cfw`

4. Arthas server find the `control connection` with the id `bvDOe8XbTM2pQWjF4cfw`
   
   then send a text frame to arthas client: `response:/?method=startTunnel&id=bvDOe8XbTM2pQWjF4cfw&clientConnectionId=AMku9EFz2gxeL2gedGOC`

5. Arthas tunnel client open a new connection to tunnel server

   URL: `ws://127.0.0.1:7777/ws/?method=openTunnel&clientConnectionId=AMku9EFz2gxeL2gedGOC&id=bvDOe8XbTM2pQWjF4cfw`
   
   This connection is `tunnel connection`

6. Arthas tunnel client start connect to local arthas agent, URL: `ws://127.0.0.1:3658/ws`

   This connection is `local connection`

7. Forward websocket frame between `tunnel connection` and `local connection`.

```
+---------+     +----------------------+       +----------------------+     +--------------+
| browser <-----> arthas tunnel server | <-----> arthas tunnel client <--- -> arthas agent |
|---------+     +----------------------+       +----------------------+     +--------------+
```
