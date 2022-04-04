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

package org.eclipse.dataspaceconnector.transfer.provision.http;

import java.net.URL;

/**
 * Provides the URL where the {@link org.eclipse.dataspaceconnector.transfer.provision.http.webhook.HttpProvisionerWebhookApiController}
 * is reachable
 */
@FunctionalInterface
public interface HttpProvisionerWebhookUrl {
    /**
     * gets the URL which the HTTP Provisioner webhook provides for out-of-process systems to call back into.
     */
    URL get();
}
