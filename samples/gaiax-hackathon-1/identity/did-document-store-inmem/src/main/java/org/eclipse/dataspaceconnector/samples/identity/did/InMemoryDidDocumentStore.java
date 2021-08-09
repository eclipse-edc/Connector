package org.eclipse.dataspaceconnector.samples.identity.did;

import org.eclipse.dataspaceconnector.iam.ion.dto.did.DidDocument;
import org.eclipse.dataspaceconnector.spi.iam.ObjectStore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InMemoryDidDocumentStore implements ObjectStore<DidDocument> {

    private final Map<String, DidDocument> inMemoryMap;

    public InMemoryDidDocumentStore() {
        inMemoryMap = new HashMap<>();
    }

    @Override
    public List<DidDocument> getAll(int limit) {
        return inMemoryMap.values().stream().sorted().limit(limit).collect(Collectors.toList());
    }

    @Override
    public List<DidDocument> getAfter(String continuationToken) {
        return null;
    }

    @Override
    public void save(DidDocument didDocument) {
        inMemoryMap.put(didDocument.getId(), didDocument);
    }
}
