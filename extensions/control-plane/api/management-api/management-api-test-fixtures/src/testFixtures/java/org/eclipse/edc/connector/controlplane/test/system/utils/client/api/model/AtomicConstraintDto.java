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
 * DTO representation of an Atomic Constraint.
 */
public final class AtomicConstraintDto extends Typed {
    private String operator;
    private String rightOperand;
    private String leftOperand;

    public AtomicConstraintDto() {
        super("AtomicConstraint");
    }

    public AtomicConstraintDto(String leftOperand, String operator, String rightOperand) {
        super("AtomicConstraint");
        this.leftOperand = leftOperand;
        this.operator = operator;
        this.rightOperand = rightOperand;
    }

    public String getLeftOperand() {
        return leftOperand;
    }

    public String getOperator() {
        return operator;
    }

    public String getRightOperand() {
        return rightOperand;
    }

}
