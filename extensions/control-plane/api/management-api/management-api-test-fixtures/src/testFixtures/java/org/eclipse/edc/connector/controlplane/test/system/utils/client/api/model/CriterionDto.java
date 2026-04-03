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

/**
 * DTO representation of a Criterion.
 */
public final class CriterionDto extends Typed {
    private final String operandLeft;
    private final String operator;
    private final Object operandRight;

    public CriterionDto(String operandLeft, String operator, Object operandRight) {
        super("Criterion");
        this.operandLeft = operandLeft;
        this.operator = operator;
        this.operandRight = operandRight;
    }


    public String getOperandLeft() {
        return operandLeft;
    }

    public String getOperator() {
        return operator;
    }

    public Object getOperandRight() {
        return operandRight;
    }

}
