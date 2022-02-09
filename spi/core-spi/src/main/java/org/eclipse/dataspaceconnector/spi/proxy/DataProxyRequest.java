package org.eclipse.dataspaceconnector.spi.proxy;

import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.jetbrains.annotations.NotNull;

public class DataProxyRequest {
    private final String proxyUrl;
    private final String contractId;
    private final DataAddress dataAddress;

    public DataProxyRequest(@NotNull String proxyUrl, @NotNull String contractId, @NotNull DataAddress dataAddress) {
        this.proxyUrl = proxyUrl;
        this.contractId = contractId;
        this.dataAddress = dataAddress;
    }

    public String getProxyUrl() {
        return proxyUrl;
    }

    public String getContractId() {
        return contractId;
    }

    public DataAddress getDataAddress() {
        return dataAddress;
    }
}
