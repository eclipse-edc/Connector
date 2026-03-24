/*
 *  Copyright (c) 2026 Think-it GmbH
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

package org.eclipse.edc.runtime.core.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.apiversion.ApiVersionService;
import org.eclipse.edc.spi.system.apiversion.VersionRecord;
import org.eclipse.edc.spi.types.TypeManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class ApiVersionServiceImpl implements ApiVersionService {

    private final TypeManager typeManager;
    private final Map<String, List<VersionRecord>> apiVersions = new ConcurrentHashMap<>();

    public ApiVersionServiceImpl(TypeManager typeManager) {
        this.typeManager = typeManager;
    }

    @Override
    public void registerVersionInfo(String apiContext, InputStream versionContent) {
        if (versionContent == null) {
            throw new EdcException("Version file not found or not readable.");
        }

        try {
            var records = typeManager.getMapper()
                    .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                    .readValue(versionContent, VersionRecord[].class);

            Stream.of(records).forEach(record -> addRecord(apiContext, record));
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

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
