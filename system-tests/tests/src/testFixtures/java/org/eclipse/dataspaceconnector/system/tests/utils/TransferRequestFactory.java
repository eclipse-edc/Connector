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

package org.eclipse.dataspaceconnector.system.tests.utils;

import java.util.function.Function;

/**
 * Pluggable definition for {@link org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation}
 * implementations to define the kind of transfer to be performed.
 */
public interface TransferRequestFactory extends Function<TransferSimulationUtils.TransferInitiationData, String> {
}
