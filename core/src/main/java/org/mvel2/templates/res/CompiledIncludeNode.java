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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;

import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateError;
import org.mvel2.templates.TemplateRuntime;
import org.mvel2.templates.util.TemplateOutputStream;

public class CompiledIncludeNode extends Node {

    private Serializable cIncludeExpression;
    private Serializable cPreExpression;
    private long fileDateStamp;
    private CompiledTemplate cFileCache;

    private ParserContext context;

    public CompiledIncludeNode(int begin, String name, char[] template, int start, int end, ParserContext context) {
        this.begin = begin;
        this.name = name;
        this.contents = template;
        this.cStart = start;
        this.cEnd = end - 1;
        this.end = end;
        this.context = context;

        int mark = captureToEOS(contents, cStart);
        cIncludeExpression = MVEL.compileExpression(contents, cStart, mark - cStart, context);
        if (mark != contents.length) {
            cPreExpression = MVEL.compileExpression(contents, ++mark, cEnd - mark, context);
        }
    }

    public static String readInFile(TemplateRuntime runtime, File file) {
        FileInputStream instream = null;
        BufferedReader in = null;
        final StringBuilder appender = new StringBuilder();

        try {
            instream = openInputStream(file);
            runtime.getRelPath().push(file.getParent());

            in = new BufferedReader(new InputStreamReader(instream, "UTF-8"));

            String currentLine;

            while ((currentLine = in.readLine()) != null) {
                appender.append(currentLine);
            }

            runtime.getRelPath().pop();
            return appender.toString();
        } catch (FileNotFoundException e) {
            throw new TemplateError("cannot include template '" + file.getPath() + "': file not found.");
        } catch (IOException e) {
            throw new TemplateError("unknown I/O exception while including '" + file.getPath() + "' (stacktrace nested)", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    throw new TemplateError("cannot close the reader on template file '" + file.getPath() + "'.");
                }
            }
            if (instream != null) {
                try {
                    instream.close();
                } catch (IOException e) {
                    throw new TemplateError("cannot close the stream on template file '" + file.getPath() + "'.");
                }
            }
        }
    }

    /**
     * Opens a {@link FileInputStream} for the specified file, else providing a
     * detail error message than simply calling <code>new FileInputStream(file)</code>.
     *
     * <p>
     * An exception is thrown if
     * <ul>
     * <li>the file parameter is null,
     * <li>the file does not exist,
     * <li>the file object exists but is a directory,
     * <li>the file exists but cannot be read.
     * </ul>
     *
     * @param file the file to open for input, can be {@code null}
     * @return a new {@link FileInputStream} for the specified file
     * @throws FileNotFoundException if the file is null or does not exist
     * @throws IOException           if the file object is a directory or cannot be read
     */
    private static FileInputStream openInputStream(final File file) throws IOException {
        if (file == null) {
            throw new FileNotFoundException("file parameter is null");
        } else if (file.exists()) {
            if (file.isDirectory()) {
                throw new IOException("File '" + file + "' exists but is a directory");
            }
            if (file.canRead() == false) {
                throw new IOException("File '" + file + "' cannot be read");
            }
        } else {
            throw new FileNotFoundException("File '" + file + "' does not exist");
        }
        return new FileInputStream(file);
    }

    public Object eval(TemplateRuntime runtime, TemplateOutputStream appender, Object ctx, VariableResolverFactory factory) {
        String file = MVEL.executeExpression(cIncludeExpression, ctx, factory, String.class);

        if (this.cPreExpression != null) {
            MVEL.executeExpression(cPreExpression, ctx, factory);
        }

        if (next != null) {
            return next.eval(runtime,
                    appender.append(String.valueOf(TemplateRuntime.eval(readFile(runtime, file, ctx, factory), ctx, factory))), ctx,
                    factory);
        } else {
            return appender.append(String.valueOf(MVEL.eval(readFile(runtime, file, ctx, factory), ctx, factory)));
        }
    }

    private String readFile(TemplateRuntime runtime, String fileName, Object ctx, VariableResolverFactory factory) {
        File file = new File(String.valueOf(runtime.getRelPath().peek()) + "/" + fileName);
        if (fileDateStamp == 0 || fileDateStamp != file.lastModified()) {
            fileDateStamp = file.lastModified();
            cFileCache = TemplateCompiler.compileTemplate(readInFile(runtime, file), context);
        }
        return String.valueOf(TemplateRuntime.execute(cFileCache, ctx, factory));
    }

    public boolean demarcate(Node terminatingNode, char[] template) {
        return false;
    }
}