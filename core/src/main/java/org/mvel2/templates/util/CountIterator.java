package org.mvel2.templates.util;

import java.util.Iterator;

/**
 * User: christopherbrock
 * Date: 10-Aug-2010
 * Time: 6:42:20 PM
 */
public class CountIterator implements Iterator {

    int cursor;
    int countTo;

    public CountIterator(int countTo) {
        this.countTo = countTo;
    }

    public boolean hasNext() {
        return cursor < countTo;
    }

    public Object next() {
        return cursor++;
    }

    public void remove() {
    }
}
