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

package org.eclipse.edc.connector.api.management.configuration;

public class ManagementApiConfiguration {
    private final String contextAlias;

    public ManagementApiConfiguration(String contextAlias) {
        this.contextAlias = contextAlias;
    }

    public String getContextAlias() {
        return contextAlias;
    }
}
