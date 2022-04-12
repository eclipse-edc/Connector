/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.dataplane.core.token;

import org.eclipse.dataspaceconnector.common.token.TokenValidationRulesRegistryImpl;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.token.DataPlaneTransferValidationRulesRegistry;

/**
 * Registry of {@link org.eclipse.dataspaceconnector.common.token.TokenValidationRule} for validation
 * of tokens provided in input of Data Plane public API.
 */
public class DataPlaneTransferValidationRulesRegistryImpl extends TokenValidationRulesRegistryImpl implements DataPlaneTransferValidationRulesRegistry {
}
