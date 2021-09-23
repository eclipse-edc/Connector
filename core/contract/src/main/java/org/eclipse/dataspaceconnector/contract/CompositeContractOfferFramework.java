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

package org.eclipse.dataspaceconnector.contract;

import org.eclipse.dataspaceconnector.spi.contract.ContractOfferFramework;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferFrameworkQuery;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferTemplate;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

class CompositeContractOfferFramework implements ContractOfferFramework {

    private final ContractOfferFrameworkLocator contractOfferFrameworkLocator;
    private final Monitor monitor;

    public CompositeContractOfferFramework(
            final ContractOfferFrameworkLocator contractOfferFrameworkLocator,
            final Monitor monitor) {
        this.contractOfferFrameworkLocator = contractOfferFrameworkLocator;
        this.monitor = monitor;
    }

    @Override
    public Stream<ContractOfferTemplate> queryTemplates(final ContractOfferFrameworkQuery query) {
        return Optional.ofNullable(query)
                .map(this::queryForTemplates)
                .orElseGet(Stream::empty);
    }

    private Stream<ContractOfferTemplate> queryForTemplates(
            final ContractOfferFrameworkQuery contractOfferFrameworkQuery) {
        return contractOfferFrameworkLocator.locate()
                .stream()
                .flatMap(prepareInvocation(contractOfferFrameworkQuery));
    }

    private Function<ContractOfferFramework, Stream<ContractOfferTemplate>> prepareInvocation(
            final ContractOfferFrameworkQuery contractOfferFrameworkQuery) {
        return contractOfferFramework -> {
            monitor.debug(String.format("Querying %s", contractOfferFramework.getClass().getName()));
            return contractOfferFramework.queryTemplates(contractOfferFrameworkQuery);
        };
    }
}
