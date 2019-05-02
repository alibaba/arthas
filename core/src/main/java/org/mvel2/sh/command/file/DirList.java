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

package org.mvel2.sh.command.file;

import java.io.File;

import org.mvel2.sh.Command;
import org.mvel2.sh.CommandException;
import org.mvel2.sh.ShellSession;

public class DirList implements Command {

    public Object execute(ShellSession session, String[] args) {
        File current = new File(session.getEnv().get("$CWD"));

        if (!current.isDirectory())
            throw new CommandException("cannot list directory : " + session.getEnv().get("$CWD") + " is not a directory");

        File[] files = current.listFiles();

        if (files.length == 0) return null;
        else {
            System.out.append("Total ").append(String.valueOf(files.length)).append("\n");
        }

        for (File file : current.listFiles()) {
            if (file.isDirectory()) {
                System.out.append(file.getName()).append("/");
            } else {
                System.out.append(file.getName());
            }
            System.out.append("\n");
        }
        System.out.flush();

        return null;
    }

    public String getDescription() {
        return "performs a list of files and directories in the current working dir.";
    }

    public String getHelp() {
        return "no help yet";
    }
}
