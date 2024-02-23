package org.eclipse.edc.connector.dataplane.framework.iam;

import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.token.spi.TokenDecorator;

import static org.eclipse.edc.connector.dataplane.framework.iam.DefaultDataPlaneAccessTokenServiceImpl.TOKEN_ID;

public class TokenIdDecorator implements TokenDecorator {
    private final String tokenId;

    public TokenIdDecorator(String tokenId) {
        this.tokenId = tokenId;
    }

    @Override
    public TokenParameters.Builder decorate(TokenParameters.Builder tokenParameters) {
        tokenParameters.claims(TOKEN_ID, tokenId);
        return tokenParameters;
    }
}
