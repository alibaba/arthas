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

package org.mvel2.templates.res;

import static org.mvel2.util.ParseTools.subset;

import java.io.Serializable;

import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.templates.TemplateRuntime;
import org.mvel2.templates.util.TemplateOutputStream;

public abstract class Node implements Serializable {

    public Node next;
    protected String name;
    protected char[] contents;
    protected int begin;
    protected int cStart;
    protected int cEnd;
    protected int end;
    protected Node terminus;

    public Node() {
    }

    public Node(int begin, String name, char[] template, int start, int end) {
        this.begin = begin;
        this.cStart = start;
        this.cEnd = end - 1;
        this.end = end;
        this.name = name;
        this.contents = template;
        //    this.contents = subset(template, this.cStart = start, (this.end = this.cEnd = end) - start - 1);
    }

    public Node(int begin, String name, char[] template, int start, int end, Node next) {
        this.name = name;
        this.begin = begin;
        this.cStart = start;
        this.cEnd = end - 1;
        this.end = end;
        this.contents = template;
        //  this.contents = subset(template, this.cStart = start, (this.end = this.cEnd = end) - start - 1);
        this.next = next;
    }

    public abstract Object eval(TemplateRuntime runtime, TemplateOutputStream appender, Object ctx, VariableResolverFactory factory);

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public char[] getContents() {
        return contents;
    }

    public void setContents(char[] contents) {
        this.contents = contents;
    }

    public int getBegin() {
        return begin;
    }

    public void setBegin(int begin) {
        this.begin = begin;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public int getCStart() {
        return cStart;
    }

    public void setCStart(int cStart) {
        this.cStart = cStart;
    }

    public int getCEnd() {
        return cEnd;
    }

    public void setCEnd(int cEnd) {
        this.cEnd = cEnd;
    }

    public boolean isOpenNode() {
        return false;
    }

    public abstract boolean demarcate(Node terminatingNode, char[] template);

    public Node getNext() {
        return next;
    }

    public Node setNext(Node next) {
        return this.next = next;
    }

    public Node getTerminus() {
        return terminus;
    }

    public void setTerminus(Node terminus) {
        this.terminus = terminus;
    }

    public void calculateContents(char[] template) {
        this.contents = subset(template, cStart, end - cStart);
    }

    public int getLength() {
        return this.end - this.begin;
    }
}
