/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.catalog.crawler;

import org.eclipse.edc.catalog.crawler.cache.query.DspCatalogRequestAction;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolRemoteMessageDispatcher;
import org.eclipse.edc.crawler.spi.CrawlerActionRegistry;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DATASPACE_PROTOCOL_HTTP_V_2025_1;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DSP_TRANSFORMER_CONTEXT_V_2025_1;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(value = CatalogCrawlerActionExtension.NAME)
public class CatalogCrawlerActionExtension implements ServiceExtension {

    public static final String NAME = "Federated Catalog Cache DSP 2025/1";

    @Inject
    private ProtocolRemoteMessageDispatcher messageDispatcher;
    @Inject
    private CrawlerActionRegistry crawlerActionRegistry;
    @Inject
    private TypeManager typeManager;
    @Inject
    private TypeTransformerRegistry registry;
    @Inject
    private JsonLd jsonLdService;
    @Inject
    private SingleParticipantContextSupplier participantContextSupplier;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var mapper = typeManager.getMapper(JSON_LD);
        var dspTransformerRegistry = registry.forContext(DSP_TRANSFORMER_CONTEXT_V_2025_1);
        var adapter = new DspCatalogRequestAction(messageDispatcher, participantContextSupplier, context.getMonitor(), mapper, dspTransformerRegistry, jsonLdService);
        crawlerActionRegistry.register(DATASPACE_PROTOCOL_HTTP_V_2025_1, adapter);
    }

}
