/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - refactoring
 *
 */

package org.eclipse.edc.connector.controlplane.catalog;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.catalog.spi.DataService;
import org.eclipse.edc.connector.controlplane.catalog.spi.DataServiceRegistry;
import org.eclipse.edc.connector.controlplane.catalog.spi.Distribution;
import org.eclipse.edc.connector.controlplane.catalog.spi.DistributionResolver;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCT_FORMAT_ATTRIBUTE;

public class DefaultDistributionResolver implements DistributionResolver {

    private final DataServiceRegistry dataServiceRegistry;
    private final DataFlowController dataFlowController;
    private final Monitor monitor;

    public DefaultDistributionResolver(DataServiceRegistry dataServiceRegistry, DataFlowController dataFlowController,
                                       Monitor monitor) {
        this.dataServiceRegistry = dataServiceRegistry;
        this.dataFlowController = dataFlowController;
        this.monitor = monitor;
    }

    @Override
    public List<Distribution> getDistributions(String protocol, Asset asset) {
        if (asset.isCatalog()) {
            var format = asset.getPropertyAsString(DCT_FORMAT_ATTRIBUTE);
            if (format == null) {
                monitor.warning("""
                        Asset %s has no 'format' property and the DataAddress type is used instead, please adapt it as the
                        DataAddress will be removed from Asset in the forthcoming versions"""
                        .formatted(asset.getId()));
                format = Optional.ofNullable(asset.getDataAddress()).map(DataAddress::getType).orElse("");
            }

            return List.of(Distribution.Builder.newInstance()
                    .format(format)
                    .dataService(DataService.Builder.newInstance()
                            .id(Base64.getUrlEncoder().encodeToString(asset.getId().getBytes()))
                            .build())
                    .build());
        }
        return dataFlowController.transferTypesFor(asset).stream()
                .map((format) -> createDistribution(asset.getParticipantContextId(), protocol, format))
                .toList();
    }

    private Distribution createDistribution(String participantContextId, String protocol, String format) {
        var builder = Distribution.Builder.newInstance().format(format);
        dataServiceRegistry.getDataServices(participantContextId, protocol).forEach(builder::dataService);
        return builder.build();
    }
}
