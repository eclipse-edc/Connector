/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
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
