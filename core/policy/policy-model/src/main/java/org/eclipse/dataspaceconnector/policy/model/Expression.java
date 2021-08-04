/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.policy.model;

/**
 * An expression that can be evaluated.
 */
public abstract class Expression {

    public interface Visitor<R> {

        R visitLiteralExpression(LiteralExpression expression);

    }

    public abstract <R> R accept(Expression.Visitor<R> visitor);

}
