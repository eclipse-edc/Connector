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
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.util.Map;

import static org.eclipse.edc.iam.decentralizedclaims.spi.DcpConstants.DCP_CONTEXT_URL;
import static org.eclipse.edc.iam.decentralizedclaims.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_0_8;
import static org.eclipse.edc.iam.decentralizedclaims.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.iam.decentralizedclaims.spi.DcpConstants.DSPACE_DCP_V_1_0_CONTEXT;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

/**
 * Provides services required to make DCP presentation requests.
 */
@Extension("DCP Presentation Request Extension")
public class DcpPresentationRequestExtension implements ServiceExtension {

    public static final String DCP_CLIENT_CONTEXT = "dcp-client";

    @Setting(description = "If set enable the dcp v0.8 namespace will be used", key = "edc.dcp.v08.forced", required = false, defaultValue = "false")
    private boolean enableDcpV08;

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
        clientTypeTransformerRegistry.register(new JsonObjectToPresentationResponseMessageTransformer(typeManager, JSON_LD, dcpNamespace()));

        return new DefaultCredentialServiceClient(httpClient, Json.createBuilderFactory(Map.of()),
                typeManager, JSON_LD, clientTypeTransformerRegistry, jsonLd, context.getMonitor(), dcpContext(), !enableDcpV08);
    }

    private JsonLdNamespace dcpNamespace() {
        return enableDcpV08 ? DSPACE_DCP_NAMESPACE_V_0_8 : DSPACE_DCP_NAMESPACE_V_1_0;
    }

    private String dcpContext() {
        return enableDcpV08 ? DCP_CONTEXT_URL : DSPACE_DCP_V_1_0_CONTEXT;
    }
}
