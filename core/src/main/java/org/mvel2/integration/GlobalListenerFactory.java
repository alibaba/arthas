package org.mvel2.integration;

import java.util.LinkedList;
import java.util.List;

public class GlobalListenerFactory {

    private static List<Listener> propertyGetListeners;
    private static List<Listener> propertySetListeners;

    public static boolean hasGetListeners() {
        return propertyGetListeners != null && !propertyGetListeners.isEmpty();
    }

    public static boolean hasSetListeners() {
        return propertySetListeners != null && !propertySetListeners.isEmpty();
    }

    public static boolean registerGetListener(Listener getListener) {
        if (propertyGetListeners == null) propertyGetListeners = new LinkedList<Listener>();
        return propertyGetListeners.add(getListener);
    }

    public static boolean registerSetListener(Listener getListener) {
        if (propertySetListeners == null) propertySetListeners = new LinkedList<Listener>();
        return propertySetListeners.add(getListener);
    }

    public static void notifyGetListeners(Object target, String name, VariableResolverFactory variableFactory) {
        if (propertyGetListeners != null) {
            for (Listener l : propertyGetListeners) {
                l.onEvent(target, name, variableFactory, null);
            }
        }
    }

    public static void notifySetListeners(Object target, String name, VariableResolverFactory variableFactory, Object value) {
        if (propertySetListeners != null) {
            for (Listener l : propertySetListeners) {
                l.onEvent(target, name, variableFactory, value);
            }
        }
    }

    public static void disposeAll() {
        propertyGetListeners = null;
        propertySetListeners = null;
    }
}
