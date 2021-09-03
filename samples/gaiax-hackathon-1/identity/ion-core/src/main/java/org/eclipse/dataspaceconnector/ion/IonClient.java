package org.eclipse.dataspaceconnector.ion;

import org.eclipse.dataspaceconnector.ion.model.IonRequest;
import org.eclipse.dataspaceconnector.ion.model.PublicKeyDescriptor;
import org.eclipse.dataspaceconnector.ion.model.ServiceDescriptor;
import org.eclipse.dataspaceconnector.ion.model.did.resolution.DidDocument;

import java.util.List;

public interface IonClient {

    Did createDid(PublicKeyDescriptor documentPublicKey, List<ServiceDescriptor> serviceDescriptors);

    Did createDid(PublicKeyDescriptor documentPublicKey, List<ServiceDescriptor> serviceDescriptors, String network);

    DidDocument submit(IonRequest request);

    DidDocument resolve(String identifier);
}
