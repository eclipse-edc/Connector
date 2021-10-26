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

import java.util.stream.Stream;

/**
 * A {@link ContractOfferFramework} yields logic to compute contractually regulated offered assets.
 *
 * <p>The {@link ContractOfferFramework} shall be implemented by extensions to
 * provide substantial contract offer templates. The {@link ContractOfferService}
 * uses several {@link ContractOfferFramework} instances to enrich its functionality.
 */
public interface ContractOfferFramework {

    Stream<ContractOfferTemplate> queryTemplates(ContractOfferFrameworkQuery query);

}
