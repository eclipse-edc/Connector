/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.transform;

import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.Objects;

public class IntegerToBigIntegerTransformer implements IdsTypeTransformer<Integer, BigInteger> {
    @Override
    public Class<Integer> getInputType() {
        return Integer.class;
    }

    @Override
    public Class<BigInteger> getOutputType() {
        return BigInteger.class;
    }

    @Override
    public @Nullable BigInteger transform(Integer object, TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        return BigInteger.valueOf(object);
    }

}
