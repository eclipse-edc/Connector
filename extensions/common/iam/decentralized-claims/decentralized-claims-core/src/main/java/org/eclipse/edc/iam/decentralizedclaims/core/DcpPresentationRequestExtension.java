/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.iam.decentralizedclaims.core;

import jakarta.json.Json;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.decentralizedclaims.core.defaults.DefaultCredentialServiceClient;
import org.eclipse.edc.iam.decentralizedclaims.lib.DefaultPresentationRequestService;
import org.eclipse.edc.iam.decentralizedclaims.service.DidCredentialServiceUrlResolver;
import org.eclipse.edc.iam.decentralizedclaims.spi.CredentialServiceClient;
import org.eclipse.edc.iam.decentralizedclaims.spi.PresentationRequestService;
import org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenService;
import org.eclipse.edc.iam.decentralizedclaims.transform.to.JsonObjectToPresentationResponseMessageTransformer;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.time.Duration;
import java.util.Map;

import static org.eclipse.edc.iam.decentralizedclaims.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.iam.decentralizedclaims.spi.DcpConstants.DSPACE_DCP_V_1_0_CONTEXT;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

/**
 * Provides services required to make DCP presentation requests.
 */
@Extension("DCP Presentation Request Extension")
public class DcpPresentationRequestExtension implements ServiceExtension {

    public static final String DCP_CLIENT_CONTEXT = "dcp-client";

    // presentations are requested while an incoming request is being verified: a slow or unresponsive Credential
    // Service must not hold the request for long. The verification fails as retryable, so the counter-party can send
    // the request again.
    @Setting(
            key = "edc.iam.credentialservice.timeout.call",
            description = "Maximum duration of a presentation query to a Credential Service, including connecting and reading the response, in ISO-8601 duration format",
            defaultValue = DefaultCredentialServiceClient.DEFAULT_CALL_TIMEOUT)
    private Duration callTimeout;

    @Setting(
            key = "edc.iam.credentialservice.retries.max",
            description = "Maximum number of retries of a presentation query that could not be sent (connection failure)",
            defaultValue = DefaultCredentialServiceClient.DEFAULT_MAX_RETRIES + "",
            min = 0)
    private int maxRetries;

    @Setting(
            key = "edc.iam.credentialservice.response.size.max",
            description = "Maximum size of a presentation query response, in bytes",
            defaultValue = DefaultCredentialServiceClient.DEFAULT_MAX_RESPONSE_BYTES + "",
            min = 1)
    private long maxResponseBytes;

    @Inject
    private SecureTokenService secureTokenService;
    @Inject
    private DidResolverRegistry didResolverRegistry;
    @Inject
    private TypeTransformerRegistry typeTransformerRegistry;
    @Inject
    private TypeManager typeManager;
    @Inject
    private EdcHttpClient httpClient;
    @Inject
    private JsonLd jsonLd;

    @Provider(isDefault = true)
    public PresentationRequestService presentationRequestService(ServiceExtensionContext context) {
        var credentialServiceUrlResolver = new DidCredentialServiceUrlResolver(didResolverRegistry);
        return new DefaultPresentationRequestService(secureTokenService, credentialServiceUrlResolver, credentialServiceClient(context));
    }

    @Provider
    public CredentialServiceClient credentialServiceClient(ServiceExtensionContext context) {
        var clientTypeTransformerRegistry = typeTransformerRegistry.forContext(DCP_CLIENT_CONTEXT);
        clientTypeTransformerRegistry.register(new JsonObjectToPresentationResponseMessageTransformer(typeManager, JSON_LD, DSPACE_DCP_NAMESPACE_V_1_0));

        var client = httpClient.withCallTimeout(callTimeout).withMaxRetries(maxRetries);
        return new DefaultCredentialServiceClient(client, Json.createBuilderFactory(Map.of()),
                typeManager, JSON_LD, clientTypeTransformerRegistry, jsonLd, context.getMonitor(), DSPACE_DCP_V_1_0_CONTEXT, maxResponseBytes);
    }

}
