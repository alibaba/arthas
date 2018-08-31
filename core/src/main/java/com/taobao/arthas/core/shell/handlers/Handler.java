package com.taobao.arthas.core.shell.handlers;

public interface Handler<E> {
    /**
     * Something has happened, so handle it.
     *
     * @param event the event to handle
     */
    void handle(E event);
}