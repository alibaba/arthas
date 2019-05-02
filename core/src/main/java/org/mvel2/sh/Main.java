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

package org.mvel2.sh;

import java.io.File;
import java.io.IOException;

import org.mvel2.MVEL;

public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length != 0) {
            MVEL.evalFile(new File(args[0]));
        } else {
            showSplash();
            new ShellSession().run();
        }
    }

    private static void showSplash() {
        System.out.println("\nMVEL-Shell (MVELSH)");
        System.out.println("Copyright (C) 2010, Christopher Brock, The Codehaus");
        System.out.println("Version " + MVEL.VERSION + "." + MVEL.VERSION_SUB + "\n");
    }
}
