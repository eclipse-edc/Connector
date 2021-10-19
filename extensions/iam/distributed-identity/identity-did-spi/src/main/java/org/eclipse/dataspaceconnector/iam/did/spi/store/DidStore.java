package org.eclipse.dataspaceconnector.iam.did.spi.store;

import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Persists {@link DidDocument}s.
 */
public interface DidStore {

    String FEATURE = "edc:did-documentstore";

    /**
     * Returns all stored documents up to the limit.
     */
    List<DidDocument> getAll(int limit);

    /**
     * Returns all documents starting from the position specified by the given token.
     */
    List<DidDocument> getAfter(String continuationToken);

    /**
     * Persists a document.
     */
    boolean save(DidDocument document);

    /**
     * Persists a collection of documents.
     */
    void saveAll(Collection<DidDocument> documents);

    /**
     * Returns the last persisted document of null if none are found.
     */
    @Nullable
    DidDocument getLatest();

    /**
     * Returns the document corresponding to the DID or null if not found.
     */
    @Nullable
    DidDocument forId(String did);
}
