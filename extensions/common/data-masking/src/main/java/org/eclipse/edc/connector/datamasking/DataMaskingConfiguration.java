/*
 *  Copyright (c) 2025 Eclipse EDC Contributors
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Eclipse EDC Contributors - Data Masking Extension
 *
 */

package org.eclipse.edc.connector.datamasking;

import org.eclipse.edc.runtime.metamodel.annotation.Settings;

/**
 * Configuration settings for the Data Masking Extension.
 */
@Settings
public class DataMaskingConfiguration {

    public static final String DEFAULT_MASK_CHARACTER = "*";
    public static final String DEFAULT_ENABLED_FIELDS = "name,phone,phoneNumber,phone_number,email,emailAddress,email_address";

    public DataMaskingConfiguration() {
    }
}
