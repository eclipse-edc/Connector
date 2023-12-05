/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.test.e2e;

import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.sql.testfixtures.PostgresqlLocalInstance;
import org.eclipse.edc.test.e2e.participant.EndToEndTransferParticipant;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.stream.Stream;

import static org.eclipse.edc.test.e2e.PostgresConstants.JDBC_URL_PREFIX;
import static org.eclipse.edc.test.e2e.PostgresConstants.PASSWORD;
import static org.eclipse.edc.test.e2e.PostgresConstants.USER;

public class PostgresUtil {

    public static void createDatabase(EndToEndTransferParticipant participant) throws ClassNotFoundException, SQLException, IOException {
        Class.forName("org.postgresql.Driver");

        var postgres = new PostgresqlLocalInstance(USER, PASSWORD, JDBC_URL_PREFIX, participant.getName());
        postgres.createDatabase();

        var scripts = Stream.of(
                "extensions/control-plane/store/sql/asset-index-sql",
                "extensions/control-plane/store/sql/contract-definition-store-sql",
                "extensions/control-plane/store/sql/contract-negotiation-store-sql",
                "extensions/control-plane/store/sql/policy-definition-store-sql",
                "extensions/control-plane/store/sql/transfer-process-store-sql",
                "extensions/data-plane/store/sql/data-plane-store-sql",
                "extensions/policy-monitor/store/sql/policy-monitor-store-sql"
        )
                .map("../../../%s/docs/schema.sql"::formatted)
                .map(Paths::get)
                .toList();

        try (var connection = postgres.getConnection(participant.getName())) {
            for (var script : scripts) {
                var sql = Files.readString(script);

                try (var statement = connection.createStatement()) {
                    statement.execute(sql);
                } catch (Exception exception) {
                    throw new EdcPersistenceException(exception.getMessage(), exception);
                }
            }
        }
    }
}
