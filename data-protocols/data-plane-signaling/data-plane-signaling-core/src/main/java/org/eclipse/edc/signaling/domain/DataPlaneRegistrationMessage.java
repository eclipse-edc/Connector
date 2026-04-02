/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.signaling.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record DataPlaneRegistrationMessage(
        String dataplaneId,
        String endpoint,
        Set<String> transferTypes,
        Set<String> labels,
        List<Map<String, Object>> authorization
) {
}
