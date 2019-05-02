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

package org.mvel2.compiler;

import static org.mvel2.MVELRuntime.execute;
import static org.mvel2.optimizers.OptimizerFactory.setThreadAccessorOptimizer;

import java.io.Serializable;

import org.mvel2.ParserConfiguration;
import org.mvel2.ast.ASTNode;
import org.mvel2.ast.TypeCast;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.ClassImportResolverFactory;
import org.mvel2.integration.impl.StackResetResolverFactory;
import org.mvel2.optimizers.AccessorOptimizer;
import org.mvel2.optimizers.OptimizerFactory;
import org.mvel2.util.ASTLinkedList;

public class CompiledExpression implements Serializable, ExecutableStatement {

    private ASTNode firstNode;

    private Class knownEgressType;
    private Class knownIngressType;

    private boolean convertableIngressEgress;
    private boolean optimized = false;
    private boolean importInjectionRequired = false;
    private boolean literalOnly;

    private Class<? extends AccessorOptimizer> accessorOptimizer;

    private String sourceName;

    private ParserConfiguration parserConfiguration;

    public CompiledExpression(ASTLinkedList astMap, String sourceName, Class egressType, ParserConfiguration parserConfiguration,
            boolean literalOnly) {
        this.firstNode = astMap.firstNode();
        this.sourceName = sourceName;
        this.knownEgressType = astMap.isSingleNode() ? astMap.firstNonSymbol().getEgressType() : egressType;
        this.literalOnly = literalOnly;
        this.parserConfiguration = parserConfiguration;
        this.importInjectionRequired = !parserConfiguration.getImports().isEmpty();
    }

    public ASTNode getFirstNode() {
        return firstNode;
    }

    public boolean isSingleNode() {
        return firstNode != null && firstNode.nextASTNode == null;
    }

    public Class getKnownEgressType() {
        return knownEgressType;
    }

    public void setKnownEgressType(Class knownEgressType) {
        this.knownEgressType = knownEgressType;
    }

    public Class getKnownIngressType() {
        return knownIngressType;
    }

    public void setKnownIngressType(Class knownIngressType) {
        this.knownIngressType = knownIngressType;
    }

    public boolean isConvertableIngressEgress() {
        return convertableIngressEgress;
    }

    public void computeTypeConversionRule() {
        if (knownIngressType != null && knownEgressType != null) {
            convertableIngressEgress = knownIngressType.isAssignableFrom(knownEgressType);
        }
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        if (!optimized) {
            setupOptimizers();
            try {
                return getValue(ctx, variableFactory);
            } finally {
                OptimizerFactory.clearThreadAccessorOptimizer();
            }
        }
        return getValue(ctx, variableFactory);
    }

    public Object getValue(Object staticContext, VariableResolverFactory factory) {
        if (!optimized) {
            setupOptimizers();
            try {
                return getValue(staticContext, factory);
            } finally {
                OptimizerFactory.clearThreadAccessorOptimizer();
            }
        }
        return getDirectValue(staticContext, factory);
    }

    public Object getDirectValue(Object staticContext, VariableResolverFactory factory) {
        return execute(false, this, staticContext, importInjectionRequired ? new ClassImportResolverFactory(parserConfiguration, factory,
                true) : new StackResetResolverFactory(factory));
    }

    private void setupOptimizers() {
        if (accessorOptimizer != null) setThreadAccessorOptimizer(accessorOptimizer);
        optimized = true;
    }

    public boolean isOptimized() {
        return optimized;
    }

    public Class<? extends AccessorOptimizer> getAccessorOptimizer() {
        return accessorOptimizer;
    }

    public String getSourceName() {
        return sourceName;
    }

    public boolean intOptimized() {
        return false;
    }

    public ParserConfiguration getParserConfiguration() {
        return parserConfiguration;
    }

    public boolean isImportInjectionRequired() {
        return importInjectionRequired;
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        return null;
    }

    public boolean isLiteralOnly() {
        return literalOnly;
    }

    public boolean isEmptyStatement() {
        return firstNode == null;
    }

    public boolean isExplicitCast() {
        return firstNode != null && firstNode instanceof TypeCast;
    }

    public String toString() {
        StringBuilder appender = new StringBuilder();
        ASTNode node = firstNode;
        while (node != null) {
            appender.append(node.toString()).append(";\n");
            node = node.nextASTNode;
        }
        return appender.toString();
    }
}
