/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.dataspaceconnector.test.e2e;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;

import java.util.Map;

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/")
public class ControlPlaneTestController {
    private final Monitor monitor;
    private final AssetLoader assetLoader;
    private final ContractDefinitionStore contractDefinitionStore;
    private final TransferProcessStore transferProcessStore;

    public ControlPlaneTestController(Monitor monitor, AssetLoader assetLoader, ContractDefinitionStore contractDefinitionStore, TransferProcessStore transferProcessStore) {
        this.monitor = monitor;
        this.assetLoader = assetLoader;
        this.contractDefinitionStore = contractDefinitionStore;
        this.transferProcessStore = transferProcessStore;
    }

    // TODO: all of these api will be replaced by data management api

    @Path("/assets")
    @POST
    public String createAsset(Map<String, Map<String, String>> properties) {
        var assetProperties = properties.get("asset");
        var asset = Asset.Builder.newInstance().properties(assetProperties).build();

        var dataAddressProperties = properties.get("dataAddress");
        var dataAddress = DataAddress.Builder.newInstance().properties(dataAddressProperties).build();
        monitor.debug("Create asset: " + asset.getId());
        assetLoader.accept(asset, dataAddress);
        return asset.getId();
    }

    @Path("/contractdefinitions")
    @POST
    public void createContractDefinition(ContractDefinition definition) {
        monitor.debug("Create contract definition: " + definition.getId());
        contractDefinitionStore.save(definition);
    }

    @Path("/transfers/{id}")
    @GET
    public TransferProcess getTransferProcess(@PathParam("id") String id) {
        return transferProcessStore.find(id);
    }

    private static class CreateAssetRequest {

        private Asset asset;
        private DataAddress dataAddress;

        private CreateAssetRequest() {
            this.asset = null;
            this.dataAddress = null;
        }

        public Asset getAsset() {
            return asset;
        }

        public void setAsset(Asset asset) {
            this.asset = asset;
        }

        public DataAddress getDataAddress() {
            return dataAddress;
        }

        public void setDataAddress(DataAddress dataAddress) {
            this.dataAddress = dataAddress;
        }

    }

}
