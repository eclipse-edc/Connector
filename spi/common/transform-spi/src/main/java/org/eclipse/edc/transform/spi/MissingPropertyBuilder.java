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
 * Reports a missing mandatory property value.
 */
public class MissingPropertyBuilder extends AbstractProblemBuilder<MissingPropertyBuilder> {
    private final TransformerContext context;

    public MissingPropertyBuilder(TransformerContext context) {
        this.context = context;
    }

    @Override
    public void report() {
        context.reportProblem(format("%s '%s' was missing", type == null ? "Property" : type + " property", property));
    }

}
