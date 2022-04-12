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

package org.eclipse.dataspaceconnector.transfer.dataplane.spi.token;

import org.eclipse.dataspaceconnector.common.token.TokenGenerationService;

/**
 * Interface used for referencing of {@link TokenGenerationService} used in Data Plane transfer module.
 */
public interface DataPlaneTransferTokenGenerator extends TokenGenerationService {
}
