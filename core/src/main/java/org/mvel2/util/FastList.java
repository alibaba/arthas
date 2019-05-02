/**
 * MVEL 2.0
 * Copyright (C) 2007 The Codehaus
 * Mike Brock, Dhanji Prasanna, John Graham, Mark Proctor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mvel2.util;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.mvel2.ImmutableElementException;

public class FastList<E> extends AbstractList<E> implements Externalizable {

    private E[] elements;
    private int size = 0;
    private boolean updated = false;

    public FastList(int size) {
        elements = (E[]) new Object[size == 0 ? 1 : size];
    }

    public FastList(E[] elements) {
        this.size = (this.elements = elements).length;
    }

    public FastList() {
        this(10);
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(size);
        for (int i = 0; i < size; i++) {
            out.writeObject(elements[i]);
        }
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        elements = (E[]) new Object[size = in.readInt()];
        for (int i = 0; i < size; i++) {
            elements[i] = (E) in.readObject();
        }
    }

    public E get(int index) {
        return (E) elements[index];
    }

    public int size() {
        return size;
    }

    public boolean add(E o) {
        if (size == elements.length) {
            increaseSize(elements.length * 2);
        }

        elements[size++] = o;
        return true;
    }

    public E set(int i, E o) {
        if (!updated) copyArray();
        E old = elements[i];
        elements[i] = o;
        return old;
    }

    public void add(int i, E o) {
        if (size == elements.length) {
            increaseSize(elements.length * 2);
        }

        for (int c = size; c != i; c--) {
            elements[c] = elements[c - 1];
        }
        elements[i] = o;
        size++;
    }

    public E remove(int i) {
        E old = elements[i];
        for (int c = i + 1; c < size; c++) {
            elements[c - 1] = elements[c];
            elements[c] = null;
        }
        size--;
        return old;
    }

    public int indexOf(Object o) {
        if (o == null) return -1;
        for (int i = 0; i < elements.length; i++) {
            if (o.equals(elements[i])) return i;
        }
        return -1;
    }

    public int lastIndexOf(Object o) {
        if (o == null) return -1;
        for (int i = elements.length - 1; i != -1; i--) {
            if (o.equals(elements[i])) return i;
        }
        return -1;
    }

    public void clear() {
        elements = (E[]) new Object[1];
        size = 0;
    }

    public boolean addAll(int i, Collection<? extends E> collection) {
        int offset = collection.size();
        ensureCapacity(offset + size);

        if (i != 0) {
            // copy forward all elements that the insertion is occuring before
            for (int c = i; c != (i + offset); c++) {
                elements[c + offset + 1] = elements[c];
            }
        }

        int c = size == 0 ? -1 : 0;
        for (E o : collection) {
            elements[offset + c++] = o;
        }

        size += offset;

        return true;
    }

    public Iterator iterator() {
        final int size = this.size;
        return new Iterator() {

            private int cursor = 0;

            public boolean hasNext() {
                return cursor < size;
            }

            public Object next() {
                return elements[cursor++];
            }

            public void remove() {
                throw new ImmutableElementException("cannot change elements in immutable list");
            }
        };

    }

    public ListIterator<E> listIterator() {
        return new ListIterator<E>() {

            private int i = -1;

            public boolean hasNext() {
                return i < size - 1;
            }

            public E next() {
                return elements[++i];
            }

            public boolean hasPrevious() {
                return i > 0;
            }

            public E previous() {
                return elements[--i];
            }

            public int nextIndex() {
                return i++;
            }

            public int previousIndex() {
                return i--;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

            public void set(E o) {
                elements[i] = o;
            }

            public void add(Object o) {
                throw new UnsupportedOperationException();
            }
        };
    }

    public ListIterator listIterator(int i) {
        return super.listIterator(i);
    }

    public List subList(int i, int i1) {
        return super.subList(i, i1);
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof List)) return false;

        ListIterator e1 = listIterator();
        ListIterator e2 = ((List) o).listIterator();
        while (e1.hasNext() && e2.hasNext()) {
            Object o1 = e1.next();
            Object o2 = e2.next();
            if (!(o1 == null ? o2 == null : o1.equals(o2))) return false;
        }
        return !(e1.hasNext() || e2.hasNext());
    }

    public int hashCode() {
        return super.hashCode();
    }

    protected void removeRange(int i, int i1) {
        throw new RuntimeException("not implemented");
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean contains(Object o) {
        return indexOf(o) != -1;
    }

    public Object[] toArray() {
        return toArray(new Object[size]);
    }

    public Object[] toArray(Object[] objects) {
        if (objects.length < size) objects = new Object[size];
        for (int i = 0; i < size; i++) {
            objects[i] = elements[i];
        }
        return objects;
    }

    public boolean remove(Object o) {
        throw new RuntimeException("not implemented");
    }

    public boolean containsAll(Collection collection) {
        throw new RuntimeException("not implemented");
    }

    public boolean addAll(Collection collection) {
        return addAll(size, collection);
    }

    public boolean removeAll(Collection collection) {
        throw new RuntimeException("not implemented");
    }

    public boolean retainAll(Collection collection) {
        throw new RuntimeException("not implemented");
    }

    private void ensureCapacity(int additional) {
        if ((size + additional) > elements.length) increaseSize((size + additional) * 2);
    }

    private void copyArray() {
        increaseSize(elements.length);
    }

    private void increaseSize(int newSize) {
        E[] newElements = (E[]) new Object[newSize];
        for (int i = 0; i < elements.length; i++)
            newElements[i] = elements[i];

        elements = newElements;

        updated = true;
    }

    public String toString() {
        return super.toString();
    }
}
