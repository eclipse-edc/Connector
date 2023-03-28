/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.connector.contract.spi.offer;

import java.util.stream.Stream;

import org.eclipse.edc.connector.contract.spi.types.offer.Dataset;
import org.jetbrains.annotations.NotNull;

public interface DatasetResolver {
    
    @NotNull
    Stream<Dataset> queryDatasets(ContractOfferQuery query);
    
}
