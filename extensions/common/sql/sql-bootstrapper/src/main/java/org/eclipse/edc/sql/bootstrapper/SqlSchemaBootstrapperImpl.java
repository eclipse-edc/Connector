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

package org.eclipse.edc.sql.bootstrapper;

import org.eclipse.edc.spi.EdcException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

public class SqlSchemaBootstrapperImpl implements SqlSchemaBootstrapper {

    private final List<QueuedStatementRecord> statements = new ArrayList<>();

    @Override
    public void addStatementFromResource(String datasourceName, String resourceName, ClassLoader classLoader) {
        try (var sqlStream = classLoader.getResourceAsStream(resourceName); var scanner = new Scanner(Objects.requireNonNull(sqlStream)).useDelimiter("\\A")) {
            var sql = scanner.next();
            statements.add(new QueuedStatementRecord(resourceName, datasourceName, sql));
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public Map<String, List<String>> getStatements() {
        var map = new HashMap<String, List<String>>();
        statements.forEach(qsr -> map.computeIfAbsent(qsr.datasourceName(), s -> new ArrayList<>()).add(qsr.sql()));
        return map;
    }
}
