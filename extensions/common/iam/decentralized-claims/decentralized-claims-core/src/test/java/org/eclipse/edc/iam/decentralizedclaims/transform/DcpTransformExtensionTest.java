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

package org.eclipse.edc.iam.decentralizedclaims.transform;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.iam.decentralizedclaims.core.DcpTransformExtension;
import org.eclipse.edc.iam.decentralizedclaims.transform.from.JsonObjectFromPresentationResponseMessageTransformer;
import org.eclipse.edc.iam.decentralizedclaims.transform.to.JsonObjectToCredentialStatusTransformer;
import org.eclipse.edc.iam.decentralizedclaims.transform.to.JsonObjectToCredentialSubjectTransformer;
import org.eclipse.edc.iam.decentralizedclaims.transform.to.JsonObjectToIssuerTransformer;
import org.eclipse.edc.iam.decentralizedclaims.transform.to.JsonObjectToPresentationQueryTransformer;
import org.eclipse.edc.iam.decentralizedclaims.transform.to.JsonObjectToPresentationResponseMessageTransformer;
import org.eclipse.edc.iam.decentralizedclaims.transform.to.JsonObjectToVerifiableCredentialTransformer;
import org.eclipse.edc.iam.decentralizedclaims.transform.to.JsonObjectToVerifiablePresentationTransformer;
import org.eclipse.edc.iam.decentralizedclaims.transform.to.JwtToVerifiableCredentialTransformer;
import org.eclipse.edc.iam.decentralizedclaims.transform.to.JwtToVerifiablePresentationTransformer;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class DcpTransformExtensionTest {

    private final TypeTransformerRegistry mockRegistry = mock();

    private final TypeManager typeManager = mock();

    @BeforeEach
    void setup(ServiceExtensionContext context) {
        when(typeManager.getMapper(JSON_LD)).thenReturn(new ObjectMapper());
        context.registerService(TypeManager.class, typeManager);
        context.registerService(TypeTransformerRegistry.class, mockRegistry);
    }

    @Test
    void initialize_assertTransformerRegistrations(DcpTransformExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(mockRegistry).register(isA(JsonObjectToCredentialStatusTransformer.class));
        verify(mockRegistry).register(isA(JsonObjectFromPresentationResponseMessageTransformer.class));
        verify(mockRegistry).register(isA(JsonObjectToPresentationResponseMessageTransformer.class));
        verify(mockRegistry).register(isA(JsonObjectToCredentialSubjectTransformer.class));
        verify(mockRegistry).register(isA(JsonObjectToIssuerTransformer.class));
        verify(mockRegistry).register(isA(JsonObjectToPresentationQueryTransformer.class));
        verify(mockRegistry).register(isA(JsonObjectToVerifiableCredentialTransformer.class));
        verify(mockRegistry).register(isA(JsonObjectToVerifiablePresentationTransformer.class));
        verify(mockRegistry).register(isA(JwtToVerifiableCredentialTransformer.class));
        verify(mockRegistry).register(isA(JwtToVerifiablePresentationTransformer.class));
    }
}