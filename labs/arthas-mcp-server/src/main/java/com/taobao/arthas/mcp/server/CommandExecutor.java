package com.taobao.arthas.mcp.server;

import java.util.Map;


public interface CommandExecutor {

    Map<String, Object> execute(String commandLine, long timeout);
}
