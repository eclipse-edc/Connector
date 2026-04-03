/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model;

import java.util.Map;

/**
 * DTO representation of a Participant Context Config.
 */
public final class ParticipantContextConfigDto extends Typed {
    private final Map<String, String> entries;
    private final Map<String, String> privateEntries;

    public ParticipantContextConfigDto(Map<String, String> entries,
                                       Map<String, String> privateEntries) {
        super("ParticipantContextConfig");
        this.entries = entries;
        this.privateEntries = privateEntries;
    }

    public ParticipantContextConfigDto(Map<String, String> entries) {
        this(entries, Map.of());
    }

    public Map<String, String> getEntries() {
        return entries;
    }

    public Map<String, String> getPrivateEntries() {
        return privateEntries;
    }

}
