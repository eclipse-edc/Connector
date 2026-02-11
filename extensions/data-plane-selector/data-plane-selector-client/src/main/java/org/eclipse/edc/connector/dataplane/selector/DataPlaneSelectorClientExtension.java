/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.selector;

import jakarta.json.Json;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.http.spi.ControlApiHttpClient;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.edc.from.JsonObjectFromDataAddressTransformer;
import org.eclipse.edc.transform.transformer.edc.from.JsonObjectFromDataPlaneInstanceTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToDataAddressTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToDataPlaneInstanceTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonValueToGenericTypeTransformer;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(DataPlaneSelectorClientExtension.NAME)
@Provides(DataPlaneSelectorService.class)
public class DataPlaneSelectorClientExtension implements ServiceExtension {

    public static final String NAME = "DataPlane Selector client";
    private static final String EDC_DPF_SELECTOR_URL = "edc.dpf.selector.url";

    @Setting(description = "DataPlane selector api URL", key = EDC_DPF_SELECTOR_URL, required = false)
    private String selectorApiUrl;

    @Inject
    private ControlApiHttpClient httpClient;
    @Inject
    private TypeManager typeManager;
    @Inject
    private TypeTransformerRegistry typeTransformerRegistry;
    @Inject
    private JsonLd jsonLd;
    @Inject(required = false)
    private DataPlaneInstanceStore dataPlaneInstanceStore;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        if (dataPlaneInstanceStore != null) {
            return;
        }

        if (selectorApiUrl == null || selectorApiUrl.isBlank()) {
            throw new EdcException(format("No setting found for key " + EDC_DPF_SELECTOR_URL));
        }

        var builderFactory = Json.createBuilderFactory(emptyMap());
        typeTransformerRegistry.register(new JsonObjectFromDataPlaneInstanceTransformer(builderFactory, typeManager, JSON_LD));
        typeTransformerRegistry.register(new JsonObjectFromDataAddressTransformer(builderFactory, typeManager, JSON_LD));
        typeTransformerRegistry.register(new JsonObjectToDataPlaneInstanceTransformer());
        typeTransformerRegistry.register(new JsonObjectToDataAddressTransformer());
        typeTransformerRegistry.register(new JsonValueToGenericTypeTransformer(typeManager, JSON_LD));

        var dataPlaneSelectorService = new RemoteDataPlaneSelectorService(httpClient, this.selectorApiUrl, typeManager, JSON_LD,
                typeTransformerRegistry, jsonLd);
        context.registerService(DataPlaneSelectorService.class, dataPlaneSelectorService);
    }

}
