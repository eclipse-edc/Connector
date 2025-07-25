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

package org.eclipse.edc.api.management;

public interface ManagementApi {

    // Transformer scope for management API
    String MANAGEMENT_API_CONTEXT = "management-api";

    String MANAGEMENT_API_V_4_ALPHA = "v4alpha";

    // JSON-LD scope for management API
    String MANAGEMENT_SCOPE = "MANAGEMENT_API";

    // JSON-LD scope for management API version 4 alpha
    String MANAGEMENT_SCOPE_V4 = MANAGEMENT_SCOPE + ":" + MANAGEMENT_API_V_4_ALPHA;

}
