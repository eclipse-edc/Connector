/*
 *  Copyright (c) 2023 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.iam.did.parser;

import org.assertj.core.api.ThrowingConsumer;
import org.eclipse.edc.iam.did.crypto.key.RsaPrivateKeyWrapper;
import org.eclipse.edc.iam.did.spi.key.PrivateKeyWrapper;

import static org.assertj.core.api.Assertions.assertThat;

public class RsaPrivateKeyWrapperParserFunctionTest extends BasePrivateKeyParserFunctionTest<PrivateKeyWrapper> {

    protected RsaPrivateKeyWrapperParserFunctionTest() {
        super(new PrivateKeyWrapperParserFunction(), "private_rsa.pem");
    }

    @Override
    protected ThrowingConsumer<PrivateKeyWrapper> verify() {
        return key -> assertThat(key).isNotNull()
                .isInstanceOf(RsaPrivateKeyWrapper.class);
    }
}
