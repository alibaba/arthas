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

public class ChangeWorkingDir implements Command {

    public Object execute(ShellSession session, String[] args) {
        File cwd = new File(session.getEnv().get("$CWD"));

        if (args.length == 0 || ".".equals(args[0])) return null;
        else if ("..".equals(args[0])) {
            if (cwd.getParentFile() != null) {
                cwd = cwd.getParentFile();
            } else {
                throw new CommandException("already at top-level directory");
            }
        } else if (args[0].charAt(0) == '/') {
            cwd = new File(args[0]);
            if (!cwd.exists()) {
                throw new CommandException("no such directory: " + args[0]);
            }
        } else {
            cwd = new File(cwd.getAbsolutePath() + "/" + args[0]);
            if (!cwd.exists()) {
                throw new CommandException("no such directory: " + args[0]);
            }
        }

        session.getEnv().put("$CWD", cwd.getAbsolutePath());

        return null;

    }

    public String getDescription() {
        return "changes the working directory";
    }

    public String getHelp() {
        return "no help yet";
    }
}
