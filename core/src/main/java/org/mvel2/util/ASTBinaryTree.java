package org.mvel2.util;

import static org.mvel2.Operator.ADD;
import static org.mvel2.Operator.AND;
import static org.mvel2.Operator.CONTAINS;
import static org.mvel2.Operator.DIV;
import static org.mvel2.Operator.EQUAL;
import static org.mvel2.Operator.GETHAN;
import static org.mvel2.Operator.GTHAN;
import static org.mvel2.Operator.INSTANCEOF;
import static org.mvel2.Operator.LETHAN;
import static org.mvel2.Operator.LTHAN;
import static org.mvel2.Operator.MULT;
import static org.mvel2.Operator.NEQUAL;
import static org.mvel2.Operator.OR;
import static org.mvel2.Operator.PTABLE;
import static org.mvel2.Operator.REGEX;
import static org.mvel2.Operator.SIMILARITY;
import static org.mvel2.Operator.SOUNDEX;
import static org.mvel2.Operator.SUB;
import static org.mvel2.Operator.TERNARY;
import static org.mvel2.Operator.TERNARY_ELSE;

import org.mvel2.ast.ASTNode;
import org.mvel2.ast.EndOfStatement;
import org.mvel2.ast.OperatorNode;

public class ASTBinaryTree {

    private ASTNode root;
    private ASTBinaryTree left;
    private ASTBinaryTree right;

    public ASTBinaryTree(ASTNode node) {
        this.root = node;
    }

    public static ASTBinaryTree buildTree(ASTIterator input) {
        ASTIterator iter = new ASTLinkedList(input.firstNode());
        ASTBinaryTree tree = new ASTBinaryTree(iter.nextNode());
        while (iter.hasMoreNodes()) {
            ASTNode node = iter.nextNode();
            if (node instanceof EndOfStatement) {
                if (iter.hasMoreNodes()) tree = new ASTBinaryTree(iter.nextNode());
            } else {
                tree = tree.append(node);
            }
        }
        return tree;
    }

    public ASTBinaryTree append(ASTNode node) {
        if (comparePrecedence(root, node) >= 0) {
            ASTBinaryTree tree = new ASTBinaryTree(node);
            tree.left = this;
            return tree;
        } else {
            if (left == null) throw new RuntimeException("Missing left node");
            if (right == null) {
                right = new ASTBinaryTree(node);
            } else {
                right = right.append(node);
            }
            return this;
        }
    }

    public Class<?> getReturnType(boolean strongTyping) {
        if (!(root instanceof OperatorNode)) return root.getEgressType();
        if (left == null || right == null) throw new RuntimeException("Malformed expression");
        Class<?> leftType = left.getReturnType(strongTyping);
        Class<?> rightType = right.getReturnType(strongTyping);
        switch (((OperatorNode) root).getOperator()) {
            case CONTAINS:
            case SOUNDEX:
            case INSTANCEOF:
            case SIMILARITY:
            case REGEX:
                return Boolean.class;
            case ADD:
                if (leftType.equals(String.class) || rightType.equals(String.class)) return String.class;
            case SUB:
            case MULT:
            case DIV:
                if (strongTyping && !CompatibilityStrategy.areEqualityCompatible(leftType, rightType))
                    throw new RuntimeException("Associative operation requires compatible types. Found " + leftType + " and " + rightType);
                return Double.class;
            case TERNARY_ELSE:
                if (strongTyping && !CompatibilityStrategy.areEqualityCompatible(leftType, rightType))
                    throw new RuntimeException("Associative operation requires compatible types. Found " + leftType + " and " + rightType);
                return leftType;
            case EQUAL:
            case NEQUAL:
                if (strongTyping && !CompatibilityStrategy.areEqualityCompatible(leftType, rightType))
                    throw new RuntimeException("Comparison operation requires compatible types. Found " + leftType + " and " + rightType);
                return Boolean.class;
            case LTHAN:
            case LETHAN:
            case GTHAN:
            case GETHAN:
                if (strongTyping && !CompatibilityStrategy.areComparisonCompatible(leftType, rightType))
                    throw new RuntimeException("Comparison operation requires compatible types. Found " + leftType + " and " + rightType);
                return Boolean.class;
            case AND:
            case OR:
                if (strongTyping) {
                    if (leftType != Boolean.class && leftType != Boolean.TYPE)
                        throw new RuntimeException("Left side of logical operation is not of type boolean. Found " + leftType);
                    if (rightType != Boolean.class && rightType != Boolean.TYPE)
                        throw new RuntimeException("Right side of logical operation is not of type boolean. Found " + rightType);
                }
                return Boolean.class;
            case TERNARY:
                if (strongTyping && leftType != Boolean.class && leftType != Boolean.TYPE)
                    throw new RuntimeException("Condition of ternary operator is not of type boolean. Found " + leftType);
                return rightType;
        }
        // TODO: should throw new RuntimeException("Unknown operator");
        // it doesn't because I am afraid I am not covering all the OperatorNode types
        return root.getEgressType();
    }

    private int comparePrecedence(ASTNode node1, ASTNode node2) {
        if (!(node1 instanceof OperatorNode) && !(node2 instanceof OperatorNode)) return 0;
        if (node1 instanceof OperatorNode && node2 instanceof OperatorNode) {
            return PTABLE[((OperatorNode) node1).getOperator()] - PTABLE[((OperatorNode) node2).getOperator()];
        }
        return node1 instanceof OperatorNode ? -1 : 1;
    }
}
