/**
 * MVEL 2.0
 * Copyright (C) 2007  MVFLEX/Valhalla Project and the Codehaus
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

package org.mvel2;

/**
 * The conversion handler interface defines the basic interface for implementing conversion handlers in MVEL.
 *
 * @see DataConversion
 */
public interface ConversionHandler {

    /**
     * Converts the passed argument to the type represented by the handler.
     *
     * @param in - the input type
     * @return - the converted type
     */
    public Object convertFrom(Object in);

    /**
     * This method is used to indicate to the runtime whehter or not the handler knows how to convert
     * from the specified type.
     *
     * @param cls - the source type
     * @return - true if the converter supports converting from the specified type.
     */
    public boolean canConvertFrom(Class cls);
}
