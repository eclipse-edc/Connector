package org.eclipse.dataspaceconnector.ion.spi;

import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolver;
import org.eclipse.dataspaceconnector.ion.spi.request.IonRequest;
import org.eclipse.dataspaceconnector.spi.system.Feature;

@Feature(IonClient.FEATURE)
public interface IonClient extends DidResolver {

    String FEATURE = "edc:identity:ion:client";

    DidDocument submit(IonRequest request);
}
