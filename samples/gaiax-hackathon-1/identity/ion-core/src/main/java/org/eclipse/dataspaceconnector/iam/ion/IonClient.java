package org.eclipse.dataspaceconnector.iam.ion;

import org.eclipse.dataspaceconnector.iam.ion.dto.PublicKeyDescriptor;
import org.eclipse.dataspaceconnector.iam.ion.dto.ServiceDescriptor;
import org.eclipse.dataspaceconnector.iam.ion.dto.did.DidDocument;
import org.eclipse.dataspaceconnector.iam.ion.model.IonRequest;

import java.util.List;

public interface IonClient {

    Did createDid(PublicKeyDescriptor documentPublicKey, List<ServiceDescriptor> serviceDescriptors);

    Did createDid(PublicKeyDescriptor documentPublicKey, List<ServiceDescriptor> serviceDescriptors, String network);

    DidDocument submit(IonRequest request);

    DidDocument resolve(String identifier);
}
