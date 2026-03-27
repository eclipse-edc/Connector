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

package org.eclipse.edc.protocol.dsp.metadata.http.api;

import jakarta.json.Json;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.protocol.dsp.version.transformer.JsonObjectFromProtocolVersionsTransformer;
import org.eclipse.edc.protocol.dsp.version.transformer.JsonObjectFromVersionsError;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import java.util.Map;

import static org.eclipse.edc.protocol.dsp.metadata.http.api.DspMetadataApiExtension.NAME;

/**
 * Provide API for the protocol versions.
 */
@Extension(NAME)
public class DspMetadataApiExtension implements ServiceExtension {

    public static final String NAME = "Dataspace Protocol Metadata Api";

    @Inject
    private WebService webService;

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Inject
    private DataspaceProfileContextRegistry profileContextRegistry;

    @Inject
    private JsonLd jsonLd;
    @Inject
    private TypeManager typeManager;

    @Inject
    private ParticipantContextService participantContextService;


    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        var jsonFactory = Json.createBuilderFactory(Map.of());

        transformerRegistry.register(new JsonObjectFromProtocolVersionsTransformer(jsonFactory));
        transformerRegistry.register(new JsonObjectFromVersionsError(jsonFactory));

        webService.registerResource(ApiContext.PROTOCOL, new DspMetadataApiController(participantContextService, profileContextRegistry, transformerRegistry));
    }

}
