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

package org.eclipse.edc.connector.api.management.secret.v1;

import jakarta.json.JsonObject;
import jakarta.ws.rs.Path;
import org.eclipse.edc.api.ApiWarnings;
import org.eclipse.edc.connector.api.management.secret.BaseSecretsApiController;
import org.eclipse.edc.connector.spi.service.SecretService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

@Path("/v1/secrets")
public class SecretsApiV1Controller extends BaseSecretsApiController {
    private final Monitor monitor;

    public SecretsApiV1Controller(SecretService service, TypeTransformerRegistry transformerRegistry, JsonObjectValidatorRegistry validator, Monitor monitor) {
        super(service, transformerRegistry, validator);
        this.monitor = monitor;
    }

    @Override
    public JsonObject createSecret(JsonObject secretJson) {
        monitor.warning(ApiWarnings.deprecationWarning("/v1", "/v3"));
        return super.createSecret(secretJson);
    }

    @Override
    public JsonObject getSecret(String id) {
        monitor.warning(ApiWarnings.deprecationWarning("/v1", "/v3"));
        return super.getSecret(id);
    }

    @Override
    public void removeSecret(String id) {
        monitor.warning(ApiWarnings.deprecationWarning("/v1", "/v3"));
        super.removeSecret(id);
    }

    @Override
    public void updateSecret(JsonObject secretJson) {
        monitor.warning(ApiWarnings.deprecationWarning("/v1", "/v3"));
        super.updateSecret(secretJson);
    }
}
