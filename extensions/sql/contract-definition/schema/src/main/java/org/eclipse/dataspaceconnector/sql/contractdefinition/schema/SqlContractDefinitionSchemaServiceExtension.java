/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.sql.contractdefinition.schema;

import org.eclipse.dataspaceconnector.spi.persistence.EdcPersistenceException;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;

import java.util.Objects;
import javax.sql.DataSource;

public class SqlContractDefinitionSchemaServiceExtension implements ServiceExtension {
    private static final String MIGRATION_LOCATION = String.format("classpath:%s", SqlContractDefinitionSchemaServiceExtension.class.getPackageName().replaceAll("\\.", "/"));

    @Inject
    private DataSourceRegistry dataSourceRegistry;

    @Inject
    private TransactionContext transactionContext;

    private String dataSourceName;

    @Override
    public void initialize(ServiceExtensionContext context) {
        dataSourceName = context.getConfig().getString(ConfigurationKeys.DATASOURCE_NAME);
    }

    @Override
    public void start() {
        DataSource dataSource = Objects.requireNonNull(
                dataSourceRegistry.resolve(dataSourceName),
                String.format("DataSource %s could not be resolved", dataSourceName));

        FlywayDataSource flywayDataSource = new FlywayDataSource(dataSource);
        String schemaHistoryTableName = getSchemaHistoryTableName(dataSourceName);

        Flyway flyway = Flyway.configure()
                .dataSource(flywayDataSource)
                .table(schemaHistoryTableName)
                .locations(MIGRATION_LOCATION)
                .load();

        transactionContext.execute(() -> {
            MigrateResult migrateResult = flyway.migrate();

            if (!migrateResult.success) {
                throw new EdcPersistenceException(String.format("Migrating DataSource %s failed: %s", dataSourceName, String.join(", ", migrateResult.warnings)));
            }
        });
    }

    private String getSchemaHistoryTableName(String suffix) {
        return String.format("flyway_schema_history_%s", suffix);
    }
}
