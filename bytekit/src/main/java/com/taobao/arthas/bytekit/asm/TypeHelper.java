/*
* JBoss, Home of Professional Open Source
* Copyright 2008-10 Red Hat and individual contributors
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*
* @authors Andrew Dinn
*/
package com.taobao.arthas.bytekit.asm;

/**
 * Helpoer class providing static methods for manipulating type and class names,
 * field and method descriptor names etc
 */
public class TypeHelper {

    public static boolean equalDescriptors(String desc1, String desc2)
    {
        int idx1 = 0, idx2 = 0;
        int len1 = desc1.length(), len2 = desc2.length();
        while (idx1 < len1) {
            // check the other has not dropped off the end
            if (idx2 == len2) {
                if ((idx1 == (len1 - 1)) && (desc1.charAt(idx1) == '$')) {
                    return true;
                }
                return false;
            }
            // check type is the same
            char char1 = desc1.charAt(idx1);
            char char2 = desc2.charAt(idx2);
            // if we have a $ at the end of the descriptor then this means any return
            // type so special case this
            if ((char1 == '$' && idx1 == len1 - 1) || (char2 == '$' && idx2 == len2 - 1)) {
                return true;
            }
            // otherwise the chars must match
            if (char1 != char2) {
                return false;
            }
            // however an L indicates a class name and we allow a classname without a package
            // to match a class name with a package
            if (char1 == 'L') {
                // ok, ensure the names must match modulo a missing package
                int end1 = idx1 + 1;
                int end2 = idx2 + 1;
                while (end1 < len1 && desc1.charAt(end1) != ';') {
                    end1++;
                }
                while (end2 < len2 && desc2.charAt(end2) != ';') {
                    end2++;
                }
                if (end1 == len1 || end2 == len2) {
                    // bad format for desc!!
                    return false;
                }
                String typeName1 = desc1.substring(idx1 + 1, end1);
                String typeName2 = desc2.substring(idx2 + 1, end2);
                if (!typeName1.equals(typeName2)) {
                    int tailIdx1 = typeName1.lastIndexOf('/');
                    int tailIdx2 = typeName2.lastIndexOf('/');
                    if (tailIdx1 > 0) {
                        if (tailIdx2 > 0) {
                            // both specify packages so they must be different types
                            return false;
                        } else {
                            // only type 1 specifies a package so type 2 should match the tail
                            if (!typeName2.equals(typeName1.substring(tailIdx1 + 1))) {
                                return false;
                            }
                        }
                    } else {
                        if (tailIdx2 > 0) {
                            // only type 2 specifies a package so type 1 should match the tail
                            if (!typeName1.equals(typeName2.substring(tailIdx2 + 1))) {
                                return false;
                            }
                        } else {
                            // neither specify packages so they must be different types
                            return false;
                        }
                    }
                }
                // skp past ';'s
                idx1 = end1;
                idx2 = end2;
            }
            idx1++;
            idx2++;
        }

        // check the other has not reached the end
        if (idx2 != len2) {
            return false;
        }

        return true;
    }
    /**
     * convert a classname from canonical form to the form used to represent it externally i.e. replace
     * all dots with slashes
     *
     * @param className the canonical name
     * @return the external name
     */
    public static String externalizeClass(String className)
    {
        return className.replace('.', '/');
    }

    /**
     * convert a classname from external form to canonical form i.e. replace
     * all slashes with dots
     *
     * @param className the external name
     * @return the canonical name
     */
    public static String internalizeClass(String className)
    {
        String result = className;
        int length = result.length();
        if (result.charAt(length - 1) == ';') {
            result = result.substring(1, length - 2);
        }
        result = result.replace('/', '.');
        return result;
    }

    /**
     * convert a type name from canonical form to the form used to represent it externally i.e.
     * replace primitive type names by the appropriate single letter types, class names
     * by the externalized class name bracketed by 'L' and ';' and array names by the
     * base type name preceded by '['.
     *
     * @param typeName the type name
     * @return the external name
     */
    public static String externalizeType(String typeName)
    {
        String externalName = "";
        String[] typeAndArrayIndices = typeName.split("\\[");
        String baseType = typeAndArrayIndices[0].trim();
        for (int i = 1; i< typeAndArrayIndices.length; i++) {
            String arrayIdx = typeAndArrayIndices[i];
            if (arrayIdx.indexOf("\\]") != 0) {
                externalName += '[';
            }
        }
        for (int i = 0; i < internalNames.length; i++) {
            if (internalNames[i].equals(baseType)) {
                externalName += externalNames[i];
                return externalName;
            }
        }

        externalName += "L" + externalizeClass(baseType) + ";";

        return externalName;
    }

    /**
     * list of well known typenames as written in Java code
     */
    final static private String[] internalNames = {
            "", /* equivalent to void */
            "void",
            "byte",
            "char",
            "short",
            "int",
            "long",
            "float",
            "double",
            "boolean",
            "Byte",
            "Character",
            "Short",
            "Integer",
            "Long",
            "Float",
            "Double",
            "String",
            "java.lang.Byte",
            "java.lang.Character",
            "java.lang.Short",
            "java.lang.Integer",
            "java.lang.Long",
            "java.lang.Float",
            "java.lang.Double",
            "java.lang.String"
    };

    /**
     * list of typenames in external form corresponding to entries ni previous list
     */
    final static private String[] externalNames = {
            "$",
            "V",
            "B",
            "C",
            "S",
            "I",
            "J",
            "F",
            "D",
            "Z",
            "Ljava/lang/Byte;",
            "Ljava/lang/Character;",
            "Ljava/lang/Short;",
            "Ljava/lang/Integer;",
            "Ljava/lang/Long;",
            "Ljava/lang/Float;",
            "Ljava/lang/Double;",
            "Ljava/lang/String;",
            "Ljava/lang/Byte;",
            "Ljava/lang/Character;",
            "Ljava/lang/Short;",
            "Ljava/lang/Integer;",
            "Ljava/lang/Long;",
            "Ljava/lang/Float;",
            "Ljava/lang/Double;",
            "Ljava/lang/String;"
    };

    /**
     * convert a method descriptor from canonical form to the form used to represent it externally
     *
     * @param desc the method descriptor which must be trimmed of any surrounding white space
     * @return an externalised form for the descriptor
     */
    public static String externalizeDescriptor(String desc)
    {
        // the descriptor will start with '(' and the arguments list should end with ')' and,
        // if it is not void be followed by a return type
        int openIdx = desc.indexOf('(');
        int closeIdx = desc.indexOf(')');
        int length = desc.length();
        if (openIdx != 0) {
            return "";
        }
        if (closeIdx < 0) {
            return "";
        }
        String retType = (closeIdx < length ? desc.substring(closeIdx + 1).trim() : "");
        String externalRetType = externalizeType(retType);
        String argString = desc.substring(1, closeIdx).trim();
        String externalArgs = "";
        if (argString.equals("")) {
            externalArgs = argString;
        } else {
            String[] args = desc.substring(1, closeIdx).trim().split(",");
            for (int i = 0; i < args.length ; i++) {
                externalArgs += externalizeType(args[i]);
            }
        }

        return "(" + externalArgs + ")" + externalRetType;
    }

    /**
     * convert a method descriptor from the form used to represent it externally to canonical form
     *
     * @param desc the method descriptor which must be trimmed of any surrounding white space and start with "(".
     * it must end either with ")" or with ") " followed by an exernalized return type
     * @return an internalised form for the descriptor, possibly followed by a space and externalized return type
     */
    public static String internalizeDescriptor(String desc)
    {
        StringBuffer buffer = new StringBuffer();
        String sepr = "";
        int argStart = desc.indexOf('(');
        int argEnd = desc.indexOf(')');
        int max = desc.length();
        if (argStart < 0 || argEnd < 0) {
            return "(...)";
        }
        int arrayCount = 0;
        boolean addSepr = false;

        buffer.append("(");

        for (int idx = argStart + 1; idx < max;) {
            char next = desc.charAt(idx);
            if (addSepr) {
                while (arrayCount > 0) {
                    buffer.append("[]");
                    arrayCount--;
                }
                buffer.append(sepr);
            }
            addSepr = true;
            switch(next) {
                case 'B':
                {
                    buffer.append("byte");
                }
                break;
                case 'C':
                {
                    buffer.append("char");
                }
                break;
                case 'S':
                {
                    buffer.append("short");
                }
                break;
                case 'I':
                {
                    buffer.append("int");
                }
                break;
                case 'J':
                {
                    buffer.append("long");
                }
                break;
                case 'Z':
                {
                    buffer.append("boolean");
                }
                break;
                case 'F':
                {
                    buffer.append("float");
                }
                break;
                case 'D':
                {
                    buffer.append("double");
                }
                case 'V':
                {
                    buffer.append("void");
                }
                break;
                case 'L':
                {
                    int tailIdx = idx+1;
                    while (tailIdx < max) {
                        char tailChar = desc.charAt(tailIdx);
                        if (tailChar == ';') {
                            break;
                        }
                        if (tailChar == '/')
                        {
                            tailChar = '.';
                        }
                        buffer.append(tailChar);
                        tailIdx++;
                    }
                    idx = tailIdx;
                }
                break;
                case '[':
                {
                    arrayCount++;
                    addSepr = false;
                }
                break;
                case ')':
                {
                    if (idx == argEnd - 1) {
                        buffer.append(")");
                    } else {
                        // leave room for return type
                        buffer.append(") ");
                    }
                    addSepr = false;
                }
                break;
                default:
                {
                    addSepr = false;
                }
            }
            idx++;
            if (idx < argEnd) {
                sepr = ",";
            } else {
                sepr = "";
            }
        }

        return buffer.toString();
    }

    /**
     * split off the method name preceding the signature and return it
     * @param targetMethod - the unqualified method name, possibly including signature
     * @return the method name
     */
    public static String parseMethodName(String targetMethod) {
        int sigIdx = targetMethod.indexOf("(");
        if (sigIdx > 0) {
            return targetMethod.substring(0, sigIdx).trim();
        } else {
            return targetMethod;
        }
    }

    /**
     * split off the signature following the method name and return it
     * @param targetMethod - the unqualified method name, possibly including signature
     * @return the signature
     */
    public static String parseMethodDescriptor(String targetMethod) {
        int descIdx = targetMethod.indexOf("(");
        if (descIdx >= 0) {
            String desc = targetMethod.substring(descIdx, targetMethod.length()).trim();
            return externalizeDescriptor(desc);
        } else {
            return "";
        }
    }
}
