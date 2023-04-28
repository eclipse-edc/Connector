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

package org.eclipse.edc.transaction.atomikos;

import org.eclipse.edc.runtime.metamodel.annotation.Setting;

/**
 * Atomikos configuration keys. For details, see https://www.atomikos.com/Documentation/JtaProperties.
 */
public interface TransactionManagerConfigurationKeys {

    @Setting(required = false, value = "The maximum timeout that is allowed for a transaction, the TransactionManager reverses the transaction if it takes time more than the specified timeout")
    String TIMEOUT = "edc.atomikos.timeout";

    @Setting(required = false, value = " The directory where Atomikos transaction logs will be stored")
    String DATA_DIR = "edc.atomikos.directory";

    @Setting(required = false, value = "Boolean flag whether to use Threaded2PC protocol")
    String THREADED2PC = "edc.atomikos.threaded2pc";

    @Setting(required = false, value = "Boolean flag whether to enable logging")
    String LOGGING = "edc.atomikos.logging";

    @Setting(required = false, value = "The interval at which Atomikos will take a checkpoint of its transaction log")
    String CHECKPOINT_INTERVAL = "edc.atomikos.checkpoint.interval";

    int DEFAULT_VALUE = -1;

    String ATOMIKOS_TM_NAME = "com.atomikos.icatch.tm_unique_name";

    String ATOMIKOS_NO_FILE = "com.atomikos.icatch.no_file";
    String ATOMIKOS_OUTPUT_DIR_PROPERTY_NAME = "com.atomikos.icatch.output_dir";
    String ATOMIKOS_LOG_BASE_DIR_PROPERTY_NAME = "com.atomikos.icatch.log_base_dir";
    String ATOMIKOS_FACTORY_KEY = "com.atomikos.icatch.service";

    String ATOMIKOS_THREADED2PC = "com.atomikos.icatch.threaded_2pc";
    String ATOMIKOS_ENABLE_LOGGING = "com.atomikos.icatch.enable_logging";
    String ATOMIKOS_CHECKPOINT_INTERVAL = "com.atomikos.icatch.checkpoint_interval";
}
