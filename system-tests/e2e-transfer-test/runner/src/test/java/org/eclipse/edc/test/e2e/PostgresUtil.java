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
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.stream.Stream;

import static org.eclipse.edc.test.e2e.PostgresConstants.JDBC_URL_PREFIX;
import static org.eclipse.edc.test.e2e.PostgresConstants.PASSWORD;
import static org.eclipse.edc.test.e2e.PostgresConstants.USER;

public class PostgresUtil {

    public static void createDatabase(EndToEndTransferParticipant consumer) throws ClassNotFoundException, SQLException, IOException {
        Class.forName("org.postgresql.Driver");

        var helper = new PostgresqlLocalInstance(USER, PASSWORD, JDBC_URL_PREFIX, consumer.getName());
        helper.createDatabase(consumer.getName());

        var controlPlaneScripts = Stream.of(
                        "asset-index-sql",
                        "contract-definition-store-sql",
                        "contract-negotiation-store-sql",
                        "policy-definition-store-sql",
                        "transfer-process-store-sql")
                .map(module -> "../../../extensions/control-plane/store/sql/" + module + "/docs/schema.sql")
                .map(Paths::get);

        var dataPlaneScripts = Stream.of("data-plane-store-sql")
                .map(module -> "../../../extensions/data-plane/store/sql/" + module + "/docs/schema.sql")
                .map(Paths::get);

        var scripts = Stream.concat(controlPlaneScripts, dataPlaneScripts).toList();

        try (var connection = DriverManager.getConnection(consumer.jdbcUrl(), USER, PASSWORD)) {
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
