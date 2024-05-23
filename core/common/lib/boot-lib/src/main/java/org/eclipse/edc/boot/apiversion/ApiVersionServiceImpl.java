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

package org.eclipse.edc.boot.apiversion;

import org.eclipse.edc.spi.system.apiversion.ApiVersionService;
import org.eclipse.edc.spi.system.apiversion.VersionRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ApiVersionServiceImpl implements ApiVersionService {
    private final Map<String, List<VersionRecord>> apiVersions = new ConcurrentHashMap<>();

    @Override
    public void addRecord(String name, VersionRecord record) {
        apiVersions.computeIfAbsent(name, s -> new ArrayList<>())
                .add(record);
    }

    @Override
    public Map<String, List<VersionRecord>> getRecords() {
        return apiVersions;
    }
}
