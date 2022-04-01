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

/**
 * Carrier for OpenAPI annotations
 */
@OpenAPIDefinition
public interface HttpProvisionerWebhookApi {
    void callWebhook(ProvisionerWebhookRequest request);
}
