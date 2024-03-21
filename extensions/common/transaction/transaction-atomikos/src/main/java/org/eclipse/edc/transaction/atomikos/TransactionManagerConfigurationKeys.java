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

/**
 * Atomikos configuration keys. For details, see https://www.atomikos.com/Documentation/JtaProperties.
 */
public interface TransactionManagerConfigurationKeys {

    String ATOMIKOS_TM_NAME = "com.atomikos.icatch.tm_unique_name";

    String ATOMIKOS_NO_FILE = "com.atomikos.icatch.no_file";
    String ATOMIKOS_OUTPUT_DIR_PROPERTY_NAME = "com.atomikos.icatch.output_dir";
    String ATOMIKOS_LOG_BASE_DIR_PROPERTY_NAME = "com.atomikos.icatch.log_base_dir";
    String ATOMIKOS_FACTORY_KEY = "com.atomikos.icatch.service";

    String ATOMIKOS_THREADED2PC = "com.atomikos.icatch.threaded_2pc";
    String ATOMIKOS_ENABLE_LOGGING = "com.atomikos.icatch.enable_logging";
    String ATOMIKOS_CHECKPOINT_INTERVAL = "com.atomikos.icatch.checkpoint_interval";
}
