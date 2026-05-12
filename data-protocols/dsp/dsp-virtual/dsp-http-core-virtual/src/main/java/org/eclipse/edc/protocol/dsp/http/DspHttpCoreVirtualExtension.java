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

package org.eclipse.edc.protocol.dsp.http;

import jakarta.ws.rs.core.UriInfo;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.protocol.dsp.spi.http.DspVirtualSubResourceLocator;
import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.protocol.spi.ParticipantProfileResolver;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.jersey.providers.jsonld.ProfileJerseyJsonLdInterceptor;
import org.eclipse.edc.web.jersey.providers.jsonld.UrlInfoRequestFilter;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import static org.eclipse.edc.protocol.dsp.http.DspHttpCoreVirtualExtension.NAME;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_CONTEXT_SEPARATOR;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_SCOPE;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(NAME)
public class DspHttpCoreVirtualExtension implements ServiceExtension {

    public static final String NAME = "Dataspace Protocol Core Virtual Extension";

    @Inject
    private JsonLd jsonLd;

    @Inject
    private TypeManager typeManager;

    @Inject
    private WebService webService;

    @Inject
    private DataspaceProfileContextRegistry dataspaceProfileContextRegistry;

    @Inject
    private ParticipantProfileResolver participantProfileResolver;


    private DspVirtualSubResourceLocator resourceLocator;

    @Override
    public void initialize(ServiceExtensionContext context) {
        webService.registerResource(ApiContext.PROTOCOL, new ProfileJerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, DSP_SCOPE, this::profileFromUri));
        webService.registerResource(ApiContext.PROTOCOL, new UrlInfoRequestFilter());
        webService.registerResource(ApiContext.PROTOCOL, new DspVirtualProfileDispatcher(participantProfileResolver, dpsVirtualSubResourceLocator()));
    }

    @Provider
    public DspVirtualSubResourceLocator dpsVirtualSubResourceLocator() {
        if (resourceLocator == null) {
            resourceLocator = new DspVirtualSubResourceLocatorImpl();
        }
        return resourceLocator;
    }

    @Override
    public void prepare() {
        dataspaceProfileContextRegistry.getProfiles().forEach(this::registerJsonLdContext);
    }

    void registerJsonLdContext(DataspaceProfileContext profileContext) {
        profileContext.jsonLdContextsUrl().forEach(ctx -> jsonLd.registerContext(ctx, DSP_SCOPE + DSP_CONTEXT_SEPARATOR + profileContext.name()));
    }

    private String profileFromUri(UriInfo uriInfo) {
        return uriInfo.getPathParameters().getFirst("profileId");
    }
}
