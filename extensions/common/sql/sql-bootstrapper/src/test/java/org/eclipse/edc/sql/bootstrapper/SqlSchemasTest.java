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

package org.eclipse.edc.sql.bootstrapper;

import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Verifies that the SQL schemas of all the modules can be applied to a database that other runtimes are using, as
 * happens when a replica starts with {@code edc.sql.schema.autocreate} enabled. A statement that needs a lock
 * conflicting with readers (e.g. {@code ALTER TABLE}, which takes an ACCESS EXCLUSIVE lock even when there is nothing to
 * change) would wait for every transaction using the table, while all the others queue up behind it.
 */
@ComponentTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
class SqlSchemasTest {

    @Test
    void schemas_shouldApplyAgain_whileTransactionReadsAllTables(PostgresqlStoreSetupExtension extension) throws SQLException {
        var schemas = schemas();
        assertThat(schemas).isNotEmpty();
        schemas.forEach(extension::runQuery);

        try (var reader = extension.getConnection(); var statement = reader.createStatement()) {
            reader.setAutoCommit(false);
            var tables = new ArrayList<String>();
            try (var resultSet = statement.executeQuery("SELECT tablename FROM pg_tables WHERE schemaname = current_schema()")) {
                while (resultSet.next()) {
                    tables.add(resultSet.getString(1));
                }
            }
            for (var table : tables) {
                statement.execute("SELECT 1 FROM " + table + " LIMIT 1");
            }

            assertThatNoException().isThrownBy(() -> extension.runQuery("SET lock_timeout = '1s';" + String.join("\n", schemas)));

            reader.rollback();
        }
    }

    private List<String> schemas() {
        try (Stream<Path> files = Files.walk(TestUtils.findBuildRoot().toPath())) {
            return files.filter(path -> path.toString().endsWith(".sql"))
                    .filter(path -> path.toString().contains("/src/main/resources/"))
                    .filter(path -> !path.toString().contains("/build/"))
                    .sorted()
                    .map(this::read)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
