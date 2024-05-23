/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.controlplane.api.management.edr.v3;


import jakarta.ws.rs.Path;
import org.eclipse.edc.connector.controlplane.api.management.edr.BaseEdrCacheApiController;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceStore;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

@Path("/v3/edrs")
public class EdrCacheApiV3Controller extends BaseEdrCacheApiController implements EdrCacheApiV3 {
    public EdrCacheApiV3Controller(EndpointDataReferenceStore edrStore, TypeTransformerRegistry transformerRegistry, JsonObjectValidatorRegistry validator, Monitor monitor) {
        super(edrStore, transformerRegistry, validator, monitor);
    }
}
