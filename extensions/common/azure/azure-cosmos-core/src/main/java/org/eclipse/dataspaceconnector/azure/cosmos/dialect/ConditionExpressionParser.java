/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.azure.cosmos.dialect;

import org.eclipse.dataspaceconnector.azure.cosmos.CosmosDocument;
import org.eclipse.dataspaceconnector.common.reflection.ReflectionUtil;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.jetbrains.annotations.NotNull;

/**
 * It's the main entry point for translating a {@link Criterion} to the right {@link ConditionExpression}
 * It chooses which will be the correct translation strategy for mapping to a CosmosDB SQL condition expression
 */
public class ConditionExpressionParser {

    private final Class<? extends CosmosDocument<?>> objectType;

    /**
     * The default constructor of {@link ConditionExpressionParser}.
     * Since the objectType is missing the default translation strategy will
     * be applied, which cannot leverage the reflection on objectType
     */
    public ConditionExpressionParser() {
        this(null);
    }

    /**
     * The default constructor of {@link ConditionExpressionParser}.
     * If the objectType is present a translation strategy based on reflection can be applied
     *
     * @param objectType The runtime type of object which can be queried.
     */
    public ConditionExpressionParser(Class<? extends CosmosDocument<?>> objectType) {
        this.objectType = objectType;
    }

    /**
     * Returns a {@link ConditionExpression} for the given parameters
     * This is an abstraction for condition rewrite rules. By default, there is no rewrite rule and
     * the {@link Criterion} will be translated as path expression. E.g. `object.name = foo'
     *
     * @param criterion    The filtering condition
     * @param objectPrefix The object prefix if any
     * @return {@link ConditionExpression}
     */
    @NotNull
    public ConditionExpression parse(Criterion criterion, String objectPrefix) {
        ConditionExpression defaultExpression = new CosmosPathConditionExpression(criterion, objectPrefix);
        if (objectType != null) {
            var wrappedType = ReflectionUtil.getSingleSuperTypeGenericArgument(objectType, CosmosDocument.class);
            if (wrappedType != null) {
                var existExpression = CosmosExistsExpression.parse(wrappedType, criterion, objectPrefix);
                if (existExpression != null) {
                    defaultExpression = existExpression;
                }
            }
        }
        return defaultExpression;
    }


}
