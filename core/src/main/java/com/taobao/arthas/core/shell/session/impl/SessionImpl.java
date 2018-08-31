package com.taobao.arthas.core.shell.session.impl;

import com.taobao.arthas.core.shell.ShellServer;
import com.taobao.arthas.core.shell.command.CommandResolver;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.system.impl.InternalCommandManager;

import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class SessionImpl implements Session {
    private final static AtomicInteger lockSequence = new AtomicInteger();
    private final static int LOCK_TX_EMPTY = -1;
    private final AtomicInteger lock = new AtomicInteger(LOCK_TX_EMPTY);

    private Map<String, Object> data = new HashMap<String, Object>();

    @Override
    public Session put(String key, Object obj) {
        if (obj == null) {
            data.remove(key);
        } else {
            data.put(key, obj);
        }
        return this;
    }

    @Override
    public <T> T get(String key) {
        return (T) data.get(key);
    }

    @Override
    public <T> T remove(String key) {
        return (T) data.remove(key);
    }

    @Override
    public boolean tryLock() {
        return lock.compareAndSet(LOCK_TX_EMPTY, lockSequence.getAndIncrement());
    }

    @Override
    public void unLock() {
        int currentLockTx = lock.get();
        if (!lock.compareAndSet(currentLockTx, LOCK_TX_EMPTY)) {
            throw new IllegalStateException();
        }
    }

    @Override
    public boolean isLocked() {
        return lock.get() != LOCK_TX_EMPTY;
    }

    @Override
    public int getLock() {
        return lock.get();
    }

    @Override
    public String getSessionId() {
        return (String) data.get(ID);
    }

    @Override
    public ShellServer getServer() {
        return (ShellServer) data.get(SERVER);
    }

    @Override
    public int getPid() {
        return (Integer) data.get(PID);
    }

    @Override
    public List<CommandResolver> getCommandResolvers() {
        InternalCommandManager commandManager = (InternalCommandManager) data.get(COMMAND_MANAGER);
        return commandManager.getResolvers();
    }

    @Override
    public Instrumentation getInstrumentation() {
        return (Instrumentation) data.get(INSTRUMENTATION);
    }
}
