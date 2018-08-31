/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.net.util;

import java.io.Serializable;
import java.util.EventListener;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 */

public class ListenerList implements Serializable, Iterable<EventListener>
{
    private static final long serialVersionUID = -1934227607974228213L;

    private final CopyOnWriteArrayList<EventListener> __listeners;

    public ListenerList()
    {
        __listeners = new CopyOnWriteArrayList<EventListener>();
    }

    public void addListener(EventListener listener)
    {
            __listeners.add(listener);
    }

    public  void removeListener(EventListener listener)
    {
            __listeners.remove(listener);
    }

    public int getListenerCount()
    {
        return __listeners.size();
    }

    /**
     * Return an {@link Iterator} for the {@link EventListener} instances.
     *
     * @return an {@link Iterator} for the {@link EventListener} instances
     * @since 2.0
     * TODO Check that this is a good defensive strategy
     */
    @Override
    public Iterator<EventListener> iterator() {
            return __listeners.iterator();
    }

}
