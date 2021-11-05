/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.contract;

import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.ContractOffer;

import java.util.stream.Stream;

/**
 * The {@link ContractOfferTemplate} resolves {@link ContractOffer}s for a stream of {@link Asset}s.
 * {@link Asset}s are resolved using the {@link AssetSelectorExpression}.
 */
public interface ContractOfferTemplate {

    Stream<ContractOffer> getTemplatedOffers(Iterable<Asset> assets);

    AssetSelectorExpression getSelectorExpression();
}
