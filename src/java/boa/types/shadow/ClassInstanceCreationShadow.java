// NOTE: This file was automatically generated - DO NOT EDIT
/*
 * Copyright 2017, Robert Dyer, Kaushik Nimmala
 *                 and Bowling Green State University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package boa.types.shadow;

import java.util.ArrayList;
import java.util.List;

import boa.compiler.ast.expressions.Expression;
import boa.compiler.ast.Factor;
import boa.compiler.ast.Identifier;
import boa.compiler.ast.Node;
import boa.compiler.ast.statements.Block;
import boa.compiler.ast.statements.IfStatement;
import boa.compiler.SymbolTable;
import boa.compiler.transforms.ASTFactory;

/**
 * A shadow type for Expression.
 *
 * @author rdyer
 * @author kaushin
 */
public class ClassInstanceCreationShadow extends boa.types.BoaShadowType  {
    /**
     * Construct a {@link ClassInstanceCreationShadow}.
     */
    public ClassInstanceCreationShadow() {
        super(new boa.types.proto.ExpressionProtoTuple());

        addShadow("new_type", new boa.types.proto.TypeProtoTuple());
        addShadow("expression", new boa.types.proto.ExpressionProtoTuple());
        addShadow("arguments", new boa.types.BoaProtoList(new boa.types.proto.ExpressionProtoTuple()));
        addShadow("type_arguments", new boa.types.BoaProtoList(new boa.types.proto.TypeProtoTuple()));
        addShadow("anonymous_class_decl", new boa.types.proto.DeclarationProtoTuple());
    }

    /** {@inheritDoc} */
    @Override
    public boolean assigns(final boa.types.BoaType that) {
        if (that instanceof boa.types.BoaShadowType)
            return shadowedType.assigns(that);

        if (!super.assigns(that))
            return false;

        return this.getClass() == that.getClass();
    }

    /** {@inheritDoc} */
    @Override
    public Node lookupCodegen(final String name, final Factor fact, final SymbolTable env) {
        if ("new_type".equals(name)) return ASTFactory.createSelector("type_1", new boa.types.proto.TypeProtoTuple(), env);
        if ("expression".equals(name)) return ASTFactory.createSelector("expression_1", new boa.types.proto.ExpressionProtoTuple(), env);
        if ("arguments".equals(name)) return ASTFactory.createSelector("expressions_1", new boa.types.BoaProtoList(new boa.types.BoaProtoList(new boa.types.proto.ExpressionProtoTuple())), env);
        if ("type_arguments".equals(name)) return ASTFactory.createSelector("types_1", new boa.types.BoaProtoList(new boa.types.BoaProtoList(new boa.types.proto.TypeProtoTuple())), env);
        if ("anonymous_class_decl".equals(name)) return ASTFactory.createSelector("declaration_1", new boa.types.proto.DeclarationProtoTuple(), env);

        throw new RuntimeException("invalid shadow field '" + name + "' in shadow type ClassInstanceCreationShadow");
    }

    /** {@inheritDoc} */
    @Override
    public Expression getKindExpression(final SymbolTable env) {
        return getKindExpression("ExpressionKind", "NEW", new boa.types.proto.enums.ExpressionKindProtoMap(), env);
    }

    /** {@inheritDoc} */
    @Override
    public IfStatement getManytoOne(final SymbolTable env, final Block b) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public List<Expression> getOneToMany(final SymbolTable env) {
        final List<Expression> l = new ArrayList<Expression>();


        return l;
    }

    /**
     * Converts a shadow type message into a concrete type message.
     *
     * @param m the shadow type message
     * @return the concrete message
     */
    public boa.types.Ast.Expression flattenMessage(final boa.types.Ast.Expression.ClassInstanceCreation m) {
        final boa.types.Ast.Expression.Builder b = boa.types.Ast.Expression.newBuilder();
        b.setKind(boa.types.Ast.Expression.ExpressionKind.NEW);
        b.setType1(m.getNewType());
        if (m.hasExpression()) b.setExpression1(m.getExpression());
        for (int i = 0; i < m.getArgumentsCount(); i++) b.addExpressions1(m.getArguments(i));
        for (int i = 0; i < m.getTypeArgumentsCount(); i++) b.addTypes1(m.getTypeArguments(i));
        if (m.hasAnonymousClassDecl()) b.setDeclaration1(m.getAnonymousClassDecl());
        return b.build();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "ClassInstanceCreation";
    }
}