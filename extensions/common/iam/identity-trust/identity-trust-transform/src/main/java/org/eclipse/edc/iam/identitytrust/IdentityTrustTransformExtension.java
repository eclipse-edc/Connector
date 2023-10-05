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

package org.eclipse.edc.iam.identitytrust;

import org.eclipse.edc.iam.identitytrust.transform.to.JsonObjectToCredentialStatusTransformer;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import static java.lang.String.format;

@Extension(value = IdentityTrustTransformExtension.NAME, categories = { "iam", "transform", "jsonld" })
public class IdentityTrustTransformExtension implements ServiceExtension {
    public static final String NAME = "Identity And Trust Transform Extension";

    @Inject
    private TypeTransformerRegistry typeTransformerRegistry;

    @Inject
    private JsonLd jsonLdService;

    @Override
    public void initialize(ServiceExtensionContext context) {
        ServiceExtension.super.initialize(context);

        getResourceUri("document" + File.separator + "credentials.v2.jsonld")
                .onSuccess(uri -> jsonLdService.registerCachedDocument("https://www.w3.org/2018/credentials/v2", uri))
                .onFailure(failure -> context.getMonitor().warning("Failed to register cached json-ld document: " + failure.getFailureDetail()));

        getResourceUri("document" + File.separator + "credentials.v1.jsonld")
                .onSuccess(uri -> jsonLdService.registerCachedDocument("https://www.w3.org/2018/credentials/v1", uri))
                .onFailure(failure -> context.getMonitor().warning("Failed to register cached json-ld document: " + failure.getFailureDetail()));

        typeTransformerRegistry.register(new JsonObjectToCredentialStatusTransformer());
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
