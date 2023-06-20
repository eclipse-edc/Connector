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

import com.nimbusds.jose.jwk.RSAKey;
import org.assertj.core.api.ThrowingConsumer;

import static org.assertj.core.api.Assertions.assertThat;

public class RsaPrivateKeyParserFunctionTest extends BasePrivateKeyParserFunctionTest<RSAKey> {
    protected RsaPrivateKeyParserFunctionTest() {
        super(new RsaPrivateKeyParserFunction(), "private_rsa.pem");
    }

    @Override
    protected ThrowingConsumer<RSAKey> verify() {
        return key -> assertThat(key).isNotNull();
    }
}
