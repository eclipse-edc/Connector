package org.eclipse.dataspaceconnector.samples.identity;

import org.eclipse.dataspaceconnector.iam.ion.dto.did.DidDocument;
import org.eclipse.dataspaceconnector.spi.iam.ObjectStore;

public interface DidStore extends ObjectStore<DidDocument> {
}
