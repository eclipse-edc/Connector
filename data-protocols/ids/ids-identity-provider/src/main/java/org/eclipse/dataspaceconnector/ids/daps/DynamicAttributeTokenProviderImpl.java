package org.eclipse.dataspaceconnector.ids.daps;

import org.eclipse.dataspaceconnector.ids.daps.client.DapsClient;
import org.eclipse.dataspaceconnector.ids.daps.client.DapsClientException;

import java.util.Objects;

public class DynamicAttributeTokenProviderImpl implements DynamicAttributeTokenProvider {
    private final DapsClient dapsClient;

    public DynamicAttributeTokenProviderImpl(
            final DapsClient dapsClient){
        Objects.requireNonNull(dapsClient);
        this.dapsClient = dapsClient;
    }

    @Override
    // TODO add cache
    public DynamicAttributeToken getDynamicAttributeToken() throws DynamicAttributeTokenException {
        try {
            return dapsClient.requestDynamicAttributeToken();
        } catch (final DapsClientException e) {
            throw new DynamicAttributeTokenException(e);
        }
    }
}
