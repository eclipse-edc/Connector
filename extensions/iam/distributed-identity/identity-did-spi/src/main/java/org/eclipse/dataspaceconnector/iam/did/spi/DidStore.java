package org.eclipse.dataspaceconnector.iam.did.spi;

import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * A DID store is intended to persistently save DIDs (or rather: {@link DidDocument} objects) within a connector.
 */
public interface DidStore {

    String FEATURE = "edc:did-documentstore";

    List<DidDocument> getAll(int limit);

    List<DidDocument> getAfter(String continuationToken);

    boolean save(DidDocument entity);

    DidDocument getLatest();

    void saveAll(Collection<DidDocument> entities);

    @Nullable
    DidDocument forId(String did);
}
