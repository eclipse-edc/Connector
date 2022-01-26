/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.transaction.atomikos;

import com.atomikos.jdbc.AtomikosDataSourceBean;
import com.atomikos.jdbc.AtomikosNonXADataSourceBean;
import com.atomikos.jdbc.internal.AbstractDataSourceBean;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;

/**
 * A DataSource registry backed by Atomikos data source beans. On creation, data source beans will automatically register as resources with the transaction manager domain.
 */
public class AtomikosDataSourceRegistry implements DataSourceRegistry {
    private Map<String, DataSource> dataSources = new HashMap<>();

    @Override
    public DataSource resolve(String name) {
        return dataSources.get(name);
    }

    @Override
    public void register(String name, DataSource dataSource) {
        throw new UnsupportedOperationException("Registering third-party data sources is not supported");
    }

    public void initialize(DataSourceConfiguration configuration) {
        if (DataSourceConfiguration.DataSourceType.XA == configuration.getDataSourceType()) {
            var bean = new AtomikosDataSourceBean();
            var name = configuration.getName();
            bean.setUniqueResourceName(name);
            bean.setXaDataSourceClassName(configuration.getDriverClass());
            Properties properties = configuration.getProperties();
            bean.setXaProperties(properties);
            setBeanProperties(configuration, bean);
            dataSources.put(name, bean);
        } else {
            var bean = new AtomikosNonXADataSourceBean();
            var name = configuration.getName();
            bean.setUniqueResourceName(name);
            bean.setDriverClassName(configuration.getDriverClass());
            bean.setUrl(configuration.getUrl());
            bean.setUser(configuration.getUsername());
            bean.setPassword(configuration.getPassword());
            setBeanProperties(configuration, bean);
            dataSources.put(name, bean);
        }
    }

    private void setBeanProperties(DataSourceConfiguration configuration, AbstractDataSourceBean bean) {
        int connectionTimeout = configuration.getConnectionTimeout();
        if (connectionTimeout != -1) {
            bean.setBorrowConnectionTimeout(connectionTimeout);
        }
        try {
            int loginTimeout = configuration.getLoginTimeout();
            if (loginTimeout != -1) {
                bean.setLoginTimeout(loginTimeout);
            }
        } catch (SQLException e) {
            throw new EdcException(e);
        }

        int interval = configuration.getMaintenanceInterval();
        if (interval != -1) {
            bean.setMaintenanceInterval(interval);
        }
        int idleTime = configuration.getMaxIdle();
        if (idleTime != -1) {
            bean.setMaxIdleTime(idleTime);
        }
        int maxPoolSize = configuration.getMaxPoolSize();
        if (maxPoolSize != -1) {
            bean.setMaxPoolSize(maxPoolSize);
        }
        int minPoolSize = configuration.getMinPoolSize();
        if (minPoolSize != -1) {
            bean.setMinPoolSize(minPoolSize);
        }
        int poolSize = configuration.getPoolSize();
        if (poolSize != -1) {
            bean.setPoolSize(poolSize);
        }
        int reapTimeout = configuration.getReap();
        if (reapTimeout != -1) {
            bean.setReapTimeout(reapTimeout);
        }
        String query = configuration.getQuery();
        if (query != null) {
            bean.setTestQuery(query);
        }
    }
}
