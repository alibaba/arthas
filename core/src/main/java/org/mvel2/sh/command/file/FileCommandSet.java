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

import java.util.HashMap;
import java.util.Map;

import org.mvel2.sh.Command;
import org.mvel2.sh.CommandSet;

public class FileCommandSet implements CommandSet {

    public Map<String, Command> load() {
        Map<String, Command> cmd = new HashMap<String, Command>();

        cmd.put("ls", new DirList());
        cmd.put("cd", new ChangeWorkingDir());
        cmd.put("pwd", new PrintWorkingDirectory());

        return cmd;
    }
}
