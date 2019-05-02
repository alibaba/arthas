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

package org.mvel2.sh.command.basic;

import java.util.Map;

import org.mvel2.sh.Command;
import org.mvel2.sh.CommandException;
import org.mvel2.sh.ShellSession;
import org.mvel2.util.StringAppender;

public class Set implements Command {

    public Object execute(ShellSession session, String[] args) {

        Map<String, String> env = session.getEnv();

        if (args.length == 0) {
            for (String var : env.keySet()) {
                System.out.println(var + " = " + env.get(var));
            }
        } else if (args.length == 1) {
            throw new CommandException("incorrect number of parameters");
        } else {
            StringAppender sbuf = new StringAppender();
            for (int i = 1; i < args.length; i++) {
                sbuf.append(args[i]);
                if (i < args.length) sbuf.append(" ");
            }

            env.put(args[0], sbuf.toString().trim());
        }

        return null;
    }

    public String getDescription() {
        return "sets an environment variable";
    }

    public String getHelp() {
        return null;
    }
}
