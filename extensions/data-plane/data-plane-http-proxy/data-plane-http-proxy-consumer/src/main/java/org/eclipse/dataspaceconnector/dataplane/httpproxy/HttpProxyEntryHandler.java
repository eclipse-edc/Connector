package org.eclipse.dataspaceconnector.dataplane.httpproxy;

import com.nimbusds.jwt.JWTClaimsSet;
import org.eclipse.dataspaceconnector.dataplane.spi.token.DataEncrypter;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.proxy.DataProxyRequest;
import org.eclipse.dataspaceconnector.spi.proxy.ProxyEntry;
import org.eclipse.dataspaceconnector.spi.proxy.ProxyEntryHandler;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.token.spi.TokenGenerationService;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

import static org.eclipse.dataspaceconnector.dataplane.spi.token.TokenSchema.CONTRACT_ID_CLAIM;
import static org.eclipse.dataspaceconnector.dataplane.spi.token.TokenSchema.DATA_ADDRESS_CLAIM;

public class HttpProxyEntryHandler implements ProxyEntryHandler {

    private final TokenGenerationService tokenGenerationService;
    private final DataEncrypter encrypter;
    private final TypeManager typeManager;

    public HttpProxyEntryHandler(TokenGenerationService tokenGenerationService, DataEncrypter encrypter, TypeManager typeManager) {
        this.tokenGenerationService = tokenGenerationService;
        this.encrypter = encrypter;
        this.typeManager = typeManager;
    }

    @Override
    public Object accept(DataProxyRequest originalRequest, ProxyEntry proxyEntry) {
        HttpProxyEntryWrapper wrapper = HttpProxyEntryWrapper.from(proxyEntry);
        Date expiration = new Date(wrapper.getExpirationEpochSeconds());

        var proxyDataAddress = DataAddress.Builder.newInstance()
                .type("http")
                .property("url", wrapper.getUrl() + "/data")
                .property("token", wrapper.getToken())
                .build();
        Result<String> result = tokenGenerationService.generate(createClaims(originalRequest.getContractId(), proxyDataAddress, expiration));
        if (result.failed()) {
            throw new EdcException("Failed to generate token");
        }
        return HttpProxyEntryWrapper.toProxyEntry("/data", result.getContent(), expiration.toInstant().getEpochSecond());
    }

    private JWTClaimsSet createClaims(@NotNull String contractId, @NotNull DataAddress dataAddress, @NotNull Date expiration) {
        String encryptedDataAddress = encrypter.encrypt(typeManager.writeValueAsString(dataAddress));
        return new JWTClaimsSet.Builder()
                .expirationTime(expiration)
                .claim(CONTRACT_ID_CLAIM, contractId)
                .claim(DATA_ADDRESS_CLAIM, encryptedDataAddress)
                .build();
    }
}
