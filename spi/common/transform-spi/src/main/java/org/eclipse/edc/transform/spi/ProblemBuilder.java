/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.transform.spi;

/**
 * Reports typed problems to the transformation context.
 */
public class ProblemBuilder {
    private TransformerContext context;

    public ProblemBuilder(TransformerContext context) {
        this.context = context;
    }

    /**
     * Reports a missing mandatory property.
     */
    public MissingPropertyBuilder missingProperty() {
        return new MissingPropertyBuilder(context);
    }

    /**
     * Reports a mandatory property whose value is null or empty.
     */
    public NullPropertyBuilder nullProperty() {
        return new NullPropertyBuilder(context);
    }

    /**
     * Reports an invalid property value.
     */
    public InvalidPropertyBuilder invalidProperty() {
        return new InvalidPropertyBuilder(context);
    }

    /**
     * Reports an attempt to read a property value that is not of the expected type.
     */
    public UnexpectedTypeBuilder unexpectedType() {
        return new UnexpectedTypeBuilder(context);
    }

}
