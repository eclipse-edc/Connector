/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.api;

import org.eclipse.edc.api.transformer.CallbackAddressDtoToCallbackAddressTransformer;
import org.eclipse.edc.api.transformer.CriterionDtoToCriterionTransformer;
import org.eclipse.edc.api.transformer.CriterionToCriterionDtoTransformer;
import org.eclipse.edc.api.transformer.QuerySpecDtoToQuerySpecTransformer;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

@Extension(value = ApiCoreExtension.NAME)
public class ApiCoreExtension implements ServiceExtension {

    public static final String NAME = "API Core";

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        transformerRegistry.register(new QuerySpecDtoToQuerySpecTransformer());
        transformerRegistry.register(new CriterionToCriterionDtoTransformer());
        transformerRegistry.register(new CriterionDtoToCriterionTransformer());
        transformerRegistry.register(new CallbackAddressDtoToCallbackAddressTransformer());
    }
}
