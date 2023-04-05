/*
 *  Copyright (c) 2023 Microsoft Corporation
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

package org.eclipse.edc.ms.dataverse;

/**
 * Constants used in Microsoft Dataverse data address properties.
 */
public class MicrosoftDataverseSchema {
    private MicrosoftDataverseSchema() {
    }

    public static final String TYPE = "MicrosoftDataverse";

    public static final String ENTITY_NAME = "entity";

    public static final String SERVICE_URI = "serviceUri";

    public static final String SERVICE_PRINCIPAL_ID = "servicePrincipalId";
}
