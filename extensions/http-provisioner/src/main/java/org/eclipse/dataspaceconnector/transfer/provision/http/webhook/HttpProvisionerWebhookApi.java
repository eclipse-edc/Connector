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

package org.eclipse.dataspaceconnector.transfer.provision.http.webhook;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DeprovisionedResource;

/**
 * Carrier for OpenAPI annotations
 */
@OpenAPIDefinition
@Tag(name = "HTTP Provisioner Webhook")
public interface HttpProvisionerWebhookApi {
    void callProvisionWebhook(@NotNull String transferProcessId, @Valid ProvisionerWebhookRequest request);

    void callDeprovisionWebhook(@NotNull String transferProcessId, @Valid DeprovisionedResource resource);
}
