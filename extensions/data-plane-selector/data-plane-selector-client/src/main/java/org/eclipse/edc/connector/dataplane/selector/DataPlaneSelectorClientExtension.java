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
import org.eclipse.edc.http.spi.ControlApiHttpClient;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.edc.from.JsonObjectFromDataAddressTransformer;
import org.eclipse.edc.transform.transformer.edc.from.JsonObjectFromDataPlaneInstanceTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToDataAddressTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToDataPlaneInstanceTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonValueToGenericTypeTransformer;

import static java.util.Collections.emptyMap;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(DataPlaneSelectorClientExtension.NAME)
public class DataPlaneSelectorClientExtension implements ServiceExtension {

    public static final String NAME = "DataPlane Selector client";

    @Setting(description = "DataPlane selector api URL", key = "edc.dpf.selector.url")
    private String selectorApiUrl;

    @Setting(description = "Defines strategy for Data Plane instance selection in case Data Plane is not embedded in current runtime", defaultValue = DataPlaneSelectorService.DEFAULT_STRATEGY, key = "edc.dataplane.client.selector.strategy")
    private String selectionStrategy;

    @Inject
    private ControlApiHttpClient httpClient;

    @Inject
    private TypeManager typeManager;

    @Inject
    private TypeTransformerRegistry typeTransformerRegistry;

    @Inject
    private JsonLd jsonLd;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var builderFactory = Json.createBuilderFactory(emptyMap());
        typeTransformerRegistry.register(new JsonObjectFromDataPlaneInstanceTransformer(builderFactory, typeManager, JSON_LD));
        typeTransformerRegistry.register(new JsonObjectFromDataAddressTransformer(builderFactory));
        typeTransformerRegistry.register(new JsonObjectToDataPlaneInstanceTransformer());
        typeTransformerRegistry.register(new JsonObjectToDataAddressTransformer());
        typeTransformerRegistry.register(new JsonValueToGenericTypeTransformer(typeManager, JSON_LD));
    }

    @Provider
    public DataPlaneSelectorService dataPlaneSelectorService(ServiceExtensionContext context) {
        return new RemoteDataPlaneSelectorService(httpClient, selectorApiUrl, typeManager, JSON_LD, typeTransformerRegistry,
                selectionStrategy, jsonLd);
    }
}
