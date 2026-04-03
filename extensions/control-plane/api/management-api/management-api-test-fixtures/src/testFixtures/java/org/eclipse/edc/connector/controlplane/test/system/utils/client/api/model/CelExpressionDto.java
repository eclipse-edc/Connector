/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

/**
 * DTO representation of a CEL Expression.
 */
public final class CelExpressionDto extends Typed {
    @JsonProperty("@id")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String id;
    private final String leftOperand;
    private final String expression;
    private final Set<String> scopes;
    private final String description;

    public CelExpressionDto(String id,
                            String leftOperand,
                            String expression,
                            Set<String> scopes,
                            String description) {
        super("CelExpression");
        this.id = id;
        this.leftOperand = leftOperand;
        this.expression = expression;
        this.scopes = scopes;
        this.description = description;
    }

    public CelExpressionDto(String leftOperand, String expression, String description) {
        this(null, leftOperand, expression, Set.of(), description);
    }

    public CelExpressionDto(String leftOperand, String expression, Set<String> scopes, String description) {
        this(null, leftOperand, expression, scopes, description);
    }

    public String getId() {
        return id;
    }

    public String getLeftOperand() {
        return leftOperand;
    }

    public String getExpression() {
        return expression;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public String getDescription() {
        return description;
    }

}
