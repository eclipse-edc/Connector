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

import static java.lang.String.format;

/**
 * Reports a mandatory property with a null value.
 */
public class NullPropertyBuilder extends AbstractProblemBuilder<NullPropertyBuilder> {
    private final TransformerContext context;

    public NullPropertyBuilder(TransformerContext context) {
        this.context = context;
    }

    @Override
    public void report() {
        context.reportProblem(format("%s '%s' was null", type == null ? "Property" : type + " property", property));
    }

}
