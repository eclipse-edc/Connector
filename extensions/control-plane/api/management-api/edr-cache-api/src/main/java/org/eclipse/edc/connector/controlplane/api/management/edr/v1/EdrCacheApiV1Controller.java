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

package org.eclipse.edc.connector.controlplane.api.management.edr.v1;


import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Path;
import org.eclipse.edc.api.ApiWarnings;
import org.eclipse.edc.connector.controlplane.api.management.edr.BaseEdrCacheApiController;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceStore;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;


@Path("/v1/edrs")
public class EdrCacheApiV1Controller extends BaseEdrCacheApiController implements EdrCacheApiV1 {
    public EdrCacheApiV1Controller(EndpointDataReferenceStore edrStore, TypeTransformerRegistry transformerRegistry, JsonObjectValidatorRegistry validator, Monitor monitor) {
        super(edrStore, transformerRegistry, validator, monitor);
    }

    @Override
    public JsonArray requestEdrEntries(JsonObject querySpecJson) {
        monitor.warning(ApiWarnings.deprecationWarning("/v1", "/v3"));
        return super.requestEdrEntries(querySpecJson);
    }

    @Override
    public JsonObject getEdrEntryDataAddress(String transferProcessId) {
        monitor.warning(ApiWarnings.deprecationWarning("/v1", "/v3"));
        return super.getEdrEntryDataAddress(transferProcessId);
    }

    @Override
    public void removeEdrEntry(String transferProcessId) {
        monitor.warning(ApiWarnings.deprecationWarning("/v1", "/v3"));
        super.removeEdrEntry(transferProcessId);
    }
}
