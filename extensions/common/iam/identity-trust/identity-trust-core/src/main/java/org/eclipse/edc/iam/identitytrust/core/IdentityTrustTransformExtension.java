/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.iam.identitytrust.core;

import org.eclipse.edc.iam.identitytrust.transform.from.JsonObjectFromPresentationQueryTransformer;
import org.eclipse.edc.iam.identitytrust.transform.from.JsonObjectFromPresentationResponseMessageTransformer;
import org.eclipse.edc.iam.identitytrust.transform.to.JsonObjectToCredentialStatusTransformer;
import org.eclipse.edc.iam.identitytrust.transform.to.JsonObjectToCredentialSubjectTransformer;
import org.eclipse.edc.iam.identitytrust.transform.to.JsonObjectToIssuerTransformer;
import org.eclipse.edc.iam.identitytrust.transform.to.JsonObjectToPresentationQueryTransformer;
import org.eclipse.edc.iam.identitytrust.transform.to.JsonObjectToPresentationResponseMessageTransformer;
import org.eclipse.edc.iam.identitytrust.transform.to.JsonObjectToVerifiableCredentialTransformer;
import org.eclipse.edc.iam.identitytrust.transform.to.JsonObjectToVerifiablePresentationTransformer;
import org.eclipse.edc.iam.identitytrust.transform.to.JwtToVerifiableCredentialTransformer;
import org.eclipse.edc.iam.identitytrust.transform.to.JwtToVerifiablePresentationTransformer;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import static java.lang.String.format;
import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DCP_CONTEXT_URL;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(value = IdentityTrustTransformExtension.NAME, categories = { "iam", "transform", "jsonld" })
public class IdentityTrustTransformExtension implements ServiceExtension {
    public static final String NAME = "Identity And Trust Transform Extension";

    @Inject
    private TypeTransformerRegistry typeTransformerRegistry;

    @Inject
    private JsonLd jsonLdService;
    @Inject
    private TypeManager typeManager;

    @Override
    public void initialize(ServiceExtensionContext context) {
        getResourceUri("document" + File.separator + "credentials.v2.jsonld")
                .onSuccess(uri -> jsonLdService.registerCachedDocument("https://www.w3.org/2018/credentials/v2", uri))
                .onFailure(failure -> context.getMonitor().warning("Failed to register cached json-ld document: " + failure.getFailureDetail()));

        getResourceUri("document" + File.separator + "credentials.v1.jsonld")
                .onSuccess(uri -> jsonLdService.registerCachedDocument("https://www.w3.org/2018/credentials/v1", uri))
                .onFailure(failure -> context.getMonitor().warning("Failed to register cached json-ld document: " + failure.getFailureDetail()));

        getResourceUri("document" + File.separator + "dcp.v08.jsonld")
                .onSuccess(uri -> jsonLdService.registerCachedDocument(DCP_CONTEXT_URL, uri))
                .onFailure(failure -> context.getMonitor().warning("Failed to register cached json-ld document: " + failure.getFailureDetail()));

        typeTransformerRegistry.register(new JsonObjectToPresentationQueryTransformer(typeManager, JSON_LD));
        typeTransformerRegistry.register(new JsonObjectToPresentationResponseMessageTransformer(typeManager, JSON_LD));
        typeTransformerRegistry.register(new JsonObjectFromPresentationQueryTransformer());
        typeTransformerRegistry.register(new JsonObjectFromPresentationResponseMessageTransformer());
        typeTransformerRegistry.register(new JsonObjectToVerifiablePresentationTransformer());
        typeTransformerRegistry.register(new JsonObjectToVerifiableCredentialTransformer());
        typeTransformerRegistry.register(new JsonObjectToIssuerTransformer());
        typeTransformerRegistry.register(new JsonObjectToCredentialSubjectTransformer());
        typeTransformerRegistry.register(new JsonObjectToCredentialStatusTransformer());
        typeTransformerRegistry.register(new JwtToVerifiablePresentationTransformer(context.getMonitor(), typeManager, JSON_LD, jsonLdService));
        typeTransformerRegistry.register(new JwtToVerifiableCredentialTransformer(context.getMonitor()));
    }

    @NotNull
    private Result<URI> getResourceUri(String name) {
        var uri = getClass().getClassLoader().getResource(name);
        if (uri == null) {
            return Result.failure(format("Cannot find resource %s", name));
        }

        try {
            return Result.success(uri.toURI());
        } catch (URISyntaxException e) {
            return Result.failure(format("Cannot read resource %s: %s", name, e.getMessage()));
        }
    }
}
