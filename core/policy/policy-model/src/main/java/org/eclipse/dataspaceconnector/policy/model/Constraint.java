/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.policy.model;

/**
 * An expression or set of expressions that refines a permission, prohibitions, or duty.
 */
public abstract class Constraint {

    public interface Visitor<R> {

        R visitAndConstraint(AndConstraint constraint);

        R visitOrConstraint(OrConstraint constraint);

        R visitXoneConstraint(XoneConstraint constraint);

        R visitAtomicConstraint(AtomicConstraint constraint);

    }

    public abstract <R> R accept(Visitor<R> visitor);


}
