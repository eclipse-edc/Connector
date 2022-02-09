package org.eclipse.dataspaceconnector.dataplane.httpproxy;

import com.nimbusds.jwt.JWTClaimsSet;
import org.eclipse.dataspaceconnector.dataplane.spi.token.DataEncrypter;
import org.eclipse.dataspaceconnector.spi.proxy.DataProxy;
import org.eclipse.dataspaceconnector.spi.proxy.DataProxyRequest;
import org.eclipse.dataspaceconnector.spi.proxy.ProxyEntry;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.token.spi.TokenGenerationService;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Date;

import static org.eclipse.dataspaceconnector.dataplane.spi.token.TokenSchema.CONTRACT_ID_CLAIM;
import static org.eclipse.dataspaceconnector.dataplane.spi.token.TokenSchema.DATA_ADDRESS_CLAIM;

public class RestDataProxy implements DataProxy {

    private final TokenGenerationService tokenGenerationService;
    private final long tokenValiditySeconds;
    private final TypeManager typeManager;
    private final DataEncrypter encrypter;

    public RestDataProxy(TokenGenerationService tokenGenerationService, long tokenValiditySeconds, TypeManager typeManager, DataEncrypter encrypter) {
        this.tokenGenerationService = tokenGenerationService;
        this.tokenValiditySeconds = tokenValiditySeconds;
        this.typeManager = typeManager;
        this.encrypter = encrypter;
    }

    @Override
    public Result<ProxyEntry> getData(DataProxyRequest request) {
        Date expiration = Date.from(Instant.now().plusSeconds(tokenValiditySeconds));
        Result<String> result = tokenGenerationService.generate(createClaims(request.getContractId(), request.getDataAddress(), expiration));
        if (result.failed()) {
            return Result.failure(result.getFailureMessages());
        }
        return Result.success(HttpProxyEntryWrapper.toProxyEntry(request.getProxyUrl(), result.getContent(), expiration.toInstant().getEpochSecond()));
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
