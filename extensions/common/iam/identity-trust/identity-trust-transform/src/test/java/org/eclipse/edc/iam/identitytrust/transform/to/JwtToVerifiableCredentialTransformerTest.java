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

package org.eclipse.edc.iam.identitytrust.transform.to;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.transform.TestData.EXAMPLE_JWT_VC;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class JwtToVerifiableCredentialTransformerTest {
    private final Monitor monitor = mock();
    private final JwtToVerifiableCredentialTransformer transformer = new JwtToVerifiableCredentialTransformer(monitor);
    private final TransformerContext context = mock();

    @Test
    void transform_success() {
        var vc = transformer.transform(EXAMPLE_JWT_VC, context);

        assertThat(vc).isNotNull();
        assertThat(vc.getTypes()).doesNotContainNull().isNotEmpty();
        assertThat(vc.getCredentialStatus()).isNotNull();
        assertThat(vc.getCredentialSubject()).doesNotContainNull().isNotEmpty();

        verifyNoInteractions(context);
    }
}