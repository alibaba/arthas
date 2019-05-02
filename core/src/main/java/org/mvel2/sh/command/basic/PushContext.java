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

import org.mvel2.MVEL;
import org.mvel2.sh.Command;
import org.mvel2.sh.ShellSession;

public class PushContext implements Command {

    public Object execute(ShellSession session, String[] args) {
        session.setCtxObject(MVEL.eval(args[0], session.getCtxObject(), session.getVariables()));
        return "Changed Context";
    }

    public String getDescription() {
        return null;
    }

    public String getHelp() {
        return null;
    }
}
