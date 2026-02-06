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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.connector.controlplane.transfer;

import jakarta.json.Json;
import org.eclipse.edc.connector.controlplane.transfer.edr.DataAddressToEndpointDataReferenceTransformer;
import org.eclipse.edc.connector.controlplane.transfer.listener.TransferProcessEventListener;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.edc.from.JsonObjectFromDataAddressTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToDataAddressTransformer;

import java.util.Collections;

import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

/**
 * Provides core data transfer services to the system.
 */
@Extension(value = TransferCoreExtension.NAME)
public class TransferCoreExtension implements ServiceExtension {

    public static final String NAME = "Transfer Core";

    @Inject
    private TransferProcessObservable observable;
    @Inject
    private EventRouter eventRouter;
    @Inject
    private TypeTransformerRegistry typeTransformerRegistry;
    @Inject
    private TypeManager typeManager;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var builderFactory = Json.createBuilderFactory(Collections.emptyMap());
        typeTransformerRegistry.register(new DataAddressToEndpointDataReferenceTransformer());
        typeTransformerRegistry.register(new JsonObjectToDataAddressTransformer());
        typeTransformerRegistry.register(new JsonObjectFromDataAddressTransformer(builderFactory, typeManager, JSON_LD));

        observable.registerListener(new TransferProcessEventListener(eventRouter));
    }

}
