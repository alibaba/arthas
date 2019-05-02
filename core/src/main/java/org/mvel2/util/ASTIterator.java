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
package org.mvel2.util;

import java.io.Serializable;

import org.mvel2.ast.ASTNode;

/**
 * The ASTIterator interface defines the functionality required by the enginer, for compiletime and runtime
 * operations.  Unlike other script implementations, MVEL does not use a completely normalized AST tree for
 * it's execution.  Instead, nodes are organized into a linear order and delivered via this iterator interface,
 * much like bytecode instructions.
 */
public interface ASTIterator extends Serializable {

    public void reset();

    public ASTNode nextNode();

    public void skipNode();

    public ASTNode peekNext();

    public ASTNode peekNode();

    public ASTNode peekLast();

    //  public boolean peekNextTokenFlags(int flags);
    public void back();

    public ASTNode nodesBack(int offset);

    public ASTNode nodesAhead(int offset);

    public boolean hasMoreNodes();

    public String showNodeChain();

    public ASTNode firstNode();

    public int size();

    public int index();

    public void finish();

    public void addTokenNode(ASTNode node);

    public void addTokenNode(ASTNode node1, ASTNode node2);

}
