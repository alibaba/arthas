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

package org.mvel2.conversion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.mvel2.ConversionHandler;

public class ListCH implements ConversionHandler {

    public Object convertFrom(Object in) {
        Class type = in.getClass();
        List newList = new ArrayList();
        if (type.isArray()) {
            newList.addAll(Arrays.asList(((Object[]) in)));
        } else if (Collection.class.isAssignableFrom(type)) {
            newList.addAll((Collection) in);
        } else if (Iterable.class.isAssignableFrom(type)) {
            for (Object o : (Iterable) in) {
                newList.add(o);
            }
        }

        return newList;
    }

    public boolean canConvertFrom(Class cls) {
        return cls.isArray() || Collection.class.isAssignableFrom(cls) || Iterable.class.isAssignableFrom(cls);
    }
}
