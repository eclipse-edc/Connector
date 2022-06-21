/*
 *  Copyright (c) 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.core.policy;

import de.fraunhofer.iais.eis.BinaryOperator;
import de.fraunhofer.iais.eis.Builder;
import de.fraunhofer.iais.eis.Constraint;
import de.fraunhofer.iais.eis.util.ConstraintViolationException;
import de.fraunhofer.iais.eis.util.RdfResource;
import de.fraunhofer.iais.eis.util.VocabUtil;

import java.net.URI;

/**
 * EDC implementation of {@link de.fraunhofer.iais.eis.ConstraintBuilder}.
 */
public class IdsConstraintBuilder implements Builder<Constraint> {

    private final IdsConstraintImpl constraintImpl;

    public IdsConstraintBuilder() {
        constraintImpl = new IdsConstraintImpl();
    }

    public IdsConstraintBuilder(URI id) {
        this();
        constraintImpl.setId(id);
    }

    public IdsConstraintBuilder leftOperand(String leftOperand) {
        constraintImpl.setLeftOperand(leftOperand);
        return this;
    }

    public IdsConstraintBuilder operator(BinaryOperator operator) {
        constraintImpl.setOperator(operator);
        return this;
    }

    public IdsConstraintBuilder rightOperand(RdfResource rightOperand) {
        constraintImpl.setRightOperand(rightOperand);
        return this;
    }

    public IdsConstraintBuilder rightOperandReference(URI rightOperandReference) {
        constraintImpl.setRightOperandReference(rightOperandReference);
        return this;
    }

    public IdsConstraintBuilder unit(URI unit) {
        constraintImpl.setUnit(unit);
        return this;
    }

    public IdsConstraintBuilder pipEndpoint(URI pipEndpoint) {
        constraintImpl.setPipEndpoint(pipEndpoint);
        return this;
    }

    @Override
    @SuppressWarnings("deprecated")
    public Constraint build() throws ConstraintViolationException {
        VocabUtil.getInstance().validate(constraintImpl);
        return constraintImpl;
    }
}
