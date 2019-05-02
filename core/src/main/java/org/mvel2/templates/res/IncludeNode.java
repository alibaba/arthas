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

import static org.mvel2.templates.util.TemplateTools.captureToEOS;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.mvel2.MVEL;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.templates.TemplateError;
import org.mvel2.templates.TemplateRuntime;
import org.mvel2.templates.util.TemplateOutputStream;

public class IncludeNode extends Node {
    //    private char[] includeExpression;
    //    private char[] preExpression;

    int includeStart;
    int includeOffset;

    int preStart;
    int preOffset;

    public IncludeNode(int begin, String name, char[] template, int start, int end) {
        this.begin = begin;
        this.name = name;
        this.contents = template;
        this.cStart = start;
        this.cEnd = end - 1;
        this.end = end;
        //this.contents = subset(template, this.cStart = start, (this.end = this.cEnd = end) - start - 1);

        int mark = captureToEOS(contents, 0);
        includeStart = cStart;
        includeOffset = mark - cStart;
        preStart = ++mark;
        preOffset = cEnd - mark;

        //this.includeExpression = subset(contents, 0, mark = captureToEOS(contents, 0));
        //        if (mark != contents.length) this.preExpression = subset(contents, ++mark, contents.length - mark);
    }

    public static String readInFile(TemplateRuntime runtime, String fileName) {
        File file = new File(String.valueOf(runtime.getRelPath().peek()) + "/" + fileName);

        try {
            FileInputStream instream = new FileInputStream(file);
            BufferedInputStream bufstream = new BufferedInputStream(instream);

            runtime.getRelPath().push(file.getParent());

            byte[] buf = new byte[10];
            int read;
            int i;

            StringBuilder appender = new StringBuilder();

            while ((read = bufstream.read(buf)) != -1) {
                for (i = 0; i < read; i++) {
                    appender.append((char) buf[i]);
                }
            }

            bufstream.close();
            instream.close();

            runtime.getRelPath().pop();

            return appender.toString();

        } catch (FileNotFoundException e) {
            throw new TemplateError("cannot include template '" + fileName + "': file not found.");
        } catch (IOException e) {
            throw new TemplateError("unknown I/O exception while including '" + fileName + "' (stacktrace nested)", e);
        }
    }

    public Object eval(TemplateRuntime runtime, TemplateOutputStream appender, Object ctx, VariableResolverFactory factory) {
        String file = MVEL.eval(contents, includeStart, includeOffset, ctx, factory, String.class);

        if (preOffset != 0) {
            MVEL.eval(contents, preStart, preOffset, ctx, factory);
        }

        if (next != null) {
            return next.eval(runtime, appender.append(String.valueOf(TemplateRuntime.eval(readInFile(runtime, file), ctx, factory))), ctx,
                    factory);
        } else {
            return appender.append(String.valueOf(MVEL.eval(readInFile(runtime, file), ctx, factory)));
        }
    }

    public boolean demarcate(Node terminatingNode, char[] template) {
        return false;
    }
}
