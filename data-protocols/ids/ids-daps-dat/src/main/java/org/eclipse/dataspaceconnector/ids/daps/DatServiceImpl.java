package org.eclipse.dataspaceconnector.ids.daps;

import org.eclipse.dataspaceconnector.ids.daps.client.DapsClient;
import org.eclipse.dataspaceconnector.ids.daps.client.DapsClientException;

import java.util.Objects;

public class DatServiceImpl implements DatService {
    private final DapsClient dapsClient;

    public DatServiceImpl(
            final DapsClient dapsClient){
        Objects.requireNonNull(dapsClient);
        this.dapsClient = dapsClient;
    }

    @Override
    // TODO add cache
    public Dat getDat() throws DatServiceException {
        try {
            return dapsClient.requestDat();
        } catch (final DapsClientException e) {
            throw new DatServiceException(e);
        }
    }
}
