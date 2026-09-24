/*
 *  Copyright (c) 2026 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial API and Implementation
 *
 */

package org.eclipse.edc.transform;

import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.edc.transform.spi.TypeTransformer;

public class TestTypeTransformer<INPUT, OUTPUT> implements TypeTransformer<INPUT, OUTPUT> {
    private final Class<INPUT> inputType;
    private final Class<OUTPUT> outputType;

    public TestTypeTransformer(Class<INPUT> inputType, Class<OUTPUT> outputType) {
        this.inputType = inputType;
        this.outputType = outputType;
    }

    @Override
    public Class<INPUT> getInputType() {
        return inputType;
    }

    @Override
    public Class<OUTPUT> getOutputType() {
        return outputType;
    }

    @Override
    public OUTPUT transform(INPUT input, TransformerContext context) {
        return null;
    }
}
