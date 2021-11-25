package org.eclipse.dataspaceconnector.transfer.httproxy;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import org.eclipse.dataspaceconnector.spi.transfer.synchronous.DataProxy;
import org.eclipse.dataspaceconnector.spi.transfer.synchronous.ProxyEntry;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class RestDataProxy implements DataProxy {
    public static final String DESTINATION_TYPE_HTTP = "http";
    private final String path;
    private final String connectorId;
    private final List<String> issuedTokens;

    public RestDataProxy(String proxyPath, String connectorId, List<String> issuedTokens) {
        path = proxyPath;
        this.connectorId = connectorId;
        this.issuedTokens = issuedTokens;
    }

    @Override
    public ProxyEntry getData(DataRequest request) {

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .expirationTime(Date.from(Instant.now().plus(10, ChronoUnit.MINUTES)))
                .notBeforeTime(new Date())
                .subject(request.getId())
                .issuer(connectorId)
                .audience(request.getConnectorAddress())
                .jwtID(UUID.randomUUID().toString())
                .build();
        var jwt = new PlainJWT(claims);

        String base64Jwt = jwt.serialize();
        issuedTokens.add(base64Jwt);
        return ProxyEntry.Builder.newInstance()
                .type(request.getDestinationType())
                .properties(Map.of("url", path + "/" + request.getAssetId(), "token", base64Jwt))
                .build();
    }

}
