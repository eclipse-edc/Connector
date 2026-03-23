/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.api.management.asset;

import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.api.management.schema.ManagementApiJsonSchema;
import org.eclipse.edc.connector.controlplane.api.management.asset.v5.AssetApiV5Controller;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.services.spi.asset.AssetService;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.participantcontext.spi.types.ParticipantResource;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.jersey.providers.jsonld.JerseyJsonLdInterceptor;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_SCOPE_V4;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(value = AssetApiV5Extension.NAME)
public class AssetApiV5Extension implements ServiceExtension {

    public static final String NAME = "Management API: Asset V5";

    @Inject
    private WebService webService;

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Inject
    private AssetService assetService;

    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;

    @Inject
    private JsonLd jsonLd;

    @Inject
    private TypeManager typeManager;
    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private ParticipantContextService participantContextService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        var managementTypeTransformerRegistry = transformerRegistry.forContext("management-api");

        authorizationService.addLookupFunction(Asset.class, this::findAsset);

        webService.registerResource(ApiContext.MANAGEMENT, new AssetApiV5Controller(assetService, managementTypeTransformerRegistry, validatorRegistry, monitor, authorizationService));
        webService.registerDynamicResource(ApiContext.MANAGEMENT, AssetApiV5Controller.class, new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, MANAGEMENT_SCOPE_V4, validatorRegistry, ManagementApiJsonSchema.V4.version()));

        authorizationService.addLookupFunction(ParticipantContext.class, this::findParticipant);

    }

    // TODO Temporary hack, to be placed into ParticipantContextManagementApiExtension once the participant context API is available
    private ParticipantResource findParticipant(String resourceId, String id) {
        if (resourceId.equals(id)) {
            return participantContextService.getParticipantContext(id).orElse(serviceFailure -> null);
        }
        return null;
    }

    private ParticipantResource findAsset(String ownerId, String assetId) {
        return assetService
                .search(QuerySpec.Builder.newInstance()
                        .filter(new Criterion("participantContextId", "=", ownerId))
                        .filter(new Criterion("id", "=", assetId))
                        .build()
                )
                .map(it -> it.stream().findFirst().orElse(null))
                .orElse(f -> null);
    }

}
