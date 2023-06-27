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

package org.eclipse.edc.connector.provision.http.webhook;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.eclipse.edc.connector.transfer.spi.types.DeprovisionedResource;

/**
 * Carrier for OpenAPI annotations
 */
@OpenAPIDefinition
@Tag(name = "HTTP Provisioner Webhook")
public interface HttpProvisionerWebhookApi {
    void callProvisionWebhook(String transferProcessId, ProvisionerWebhookRequest request);

    void callDeprovisionWebhook(String transferProcessId, DeprovisionedResource resource);
}
