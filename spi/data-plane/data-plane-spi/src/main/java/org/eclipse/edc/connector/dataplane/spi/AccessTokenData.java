/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.dataplane.spi;

import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.types.domain.DataAddress;

/**
 * Container object for a {@link ClaimToken} and a {@link DataAddress} that the data plane uses to keep track of currently
 * all access tokens that are currently valid.
 */
public record AccessTokenData(ClaimToken claimToken, DataAddress dataAddress, String id) {

}
