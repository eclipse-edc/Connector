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

import com.atomikos.icatch.config.UserTransactionServiceImp;
import com.atomikos.icatch.jta.TransactionManagerImp;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import javax.transaction.SystemException;

import static org.eclipse.dataspaceconnector.transaction.atomikos.TransactionManagerConfigurationKeys.ATOMIKOS_CHECKPOINT_INTERVAL;
import static org.eclipse.dataspaceconnector.transaction.atomikos.TransactionManagerConfigurationKeys.ATOMIKOS_ENABLE_LOGGING;
import static org.eclipse.dataspaceconnector.transaction.atomikos.TransactionManagerConfigurationKeys.ATOMIKOS_FACTORY_KEY;
import static org.eclipse.dataspaceconnector.transaction.atomikos.TransactionManagerConfigurationKeys.ATOMIKOS_LOG_BASE_DIR_PROPERTY_NAME;
import static org.eclipse.dataspaceconnector.transaction.atomikos.TransactionManagerConfigurationKeys.ATOMIKOS_NO_FILE;
import static org.eclipse.dataspaceconnector.transaction.atomikos.TransactionManagerConfigurationKeys.ATOMIKOS_OUTPUT_DIR_PROPERTY_NAME;
import static org.eclipse.dataspaceconnector.transaction.atomikos.TransactionManagerConfigurationKeys.ATOMIKOS_THREADED2PC;
import static org.eclipse.dataspaceconnector.transaction.atomikos.TransactionManagerConfigurationKeys.ATOMIKOS_TM_NAME;

/**
 * Manages the Atomikos transaction manager and its associated services.
 */
public class AtomikosTransactionPlatform {
    private static final String FACTORY_VALUE = "com.atomikos.icatch.standalone.UserTransactionServiceFactory";

    private TransactionManagerConfiguration configuration;
    private UserTransactionServiceImp transactionService;
    private TransactionManagerImp transactionManager;

    public AtomikosTransactionPlatform(TransactionManagerConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Starts transaction recovery and initializes the transaction manager. Note this must be called *after* transactional resources have been registered to
     * enable recovery.
     */
    public void recover() {
        var properties = initializeProperties();
        transactionManager = (TransactionManagerImp) TransactionManagerImp.getTransactionManager();
        if (transactionManager == null) {
            transactionService = new UserTransactionServiceImp(properties);
            transactionService.init(properties);
            transactionManager = (TransactionManagerImp) TransactionManagerImp.getTransactionManager();
        }
        if (configuration.getTimeout() != -1) {
            try {
                transactionManager.setTransactionTimeout(configuration.getTimeout());
            } catch (SystemException e) {
                throw new EdcException(e);
            }
        }
    }

    /**
     * Called when the runtime shuts down, ensure transactions are properly completed.
     */
    public void stop() {
        if (transactionService != null) {
            transactionService.shutdown(false);
            transactionService = null;
        }
    }

    /**
     * Returns the configured transaction manager.
     */
    public TransactionManagerImp getTransactionManager() {
        return transactionManager;
    }

    @NotNull
    private Properties initializeProperties() {
        // disable transactions.properties search by the transaction manager as they are supplied directly
        System.setProperty(ATOMIKOS_NO_FILE, "true");

        // configure mandatory value
        System.setProperty(ATOMIKOS_FACTORY_KEY, FACTORY_VALUE);

        var properties = new Properties();

        var dataDir = configuration.getDataDir();

        // set the TM name
        var name = configuration.getName();
        properties.setProperty(ATOMIKOS_TM_NAME, name);

        var path = initializeTransactionLogPath(dataDir);
        properties.setProperty(ATOMIKOS_OUTPUT_DIR_PROPERTY_NAME, path);
        properties.setProperty(ATOMIKOS_LOG_BASE_DIR_PROPERTY_NAME, path);

        properties.setProperty(ATOMIKOS_THREADED2PC, Boolean.toString(configuration.getSingleThreaded2Pc()));
        properties.setProperty(ATOMIKOS_ENABLE_LOGGING, Boolean.toString(configuration.getEnableLogging()));
        if (configuration.getCheckPointInterval() != -1) {
            properties.setProperty(ATOMIKOS_CHECKPOINT_INTERVAL, Long.toString(configuration.getCheckPointInterval()));
        }
        return properties;
    }

    @NotNull
    private String initializeTransactionLogPath(String dataDir) {
        try {
            var trxDir = new File(dataDir, "transactions");
            if (!trxDir.exists()) {
                trxDir.mkdirs();
            }
            return trxDir.getCanonicalPath();
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

}
