package org.eclipse.dataspaceconnector.ids.transform;

import de.fraunhofer.iais.eis.ConnectorEndpoint;
import de.fraunhofer.iais.eis.ConnectorEndpointBuilder;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Objects;

public class UriToConnectorEndpointTransformer implements IdsTypeTransformer<URI, ConnectorEndpoint> {
    @Override
    public Class<URI> getInputType() {
        return URI.class;
    }

    @Override
    public Class<ConnectorEndpoint> getOutputType() {
        return ConnectorEndpoint.class;
    }

    @Override
    public @Nullable ConnectorEndpoint transform(URI object, TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        ConnectorEndpointBuilder endpoint = new ConnectorEndpointBuilder(object);
        endpoint._accessURL_(object);

        return endpoint.build();
    }
}
