/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.iam.ion;

import com.nimbusds.jose.jwk.JWK;
import org.eclipse.dataspaceconnector.iam.ion.crypto.KeyPairFactory;
import org.eclipse.dataspaceconnector.iam.ion.dto.DidState;
import org.eclipse.dataspaceconnector.iam.ion.dto.IonState;
import org.eclipse.dataspaceconnector.iam.ion.dto.PublicKeyDescriptor;
import org.eclipse.dataspaceconnector.iam.ion.dto.ServiceDescriptor;
import org.eclipse.dataspaceconnector.iam.ion.model.*;
import org.eclipse.dataspaceconnector.iam.ion.util.JsonCanonicalizer;
import org.eclipse.dataspaceconnector.iam.ion.util.MultihashHelper;

import java.util.*;

import static org.eclipse.dataspaceconnector.iam.ion.model.DidOperationType.*;

class DidImpl implements Did {

    private final List<DidOperation> operations;
    private final String network;
    private String longForm;

    public DidImpl(PublicKeyDescriptor publicKey, List<ServiceDescriptor> serviceDescriptors, String network) {
        this(publicKey, serviceDescriptors, new ArrayList<>(), network);
    }

    public DidImpl(PublicKeyDescriptor publicKey, List<ServiceDescriptor> serviceDescriptors, List<DidOperation> operations, String network) {
        this.operations = operations;
        this.network = network;

        if (this.operations.isEmpty()) {

            var map = new HashMap<String, Object>();
            map.put("publicKeys", Collections.singletonList(publicKey));
            map.put("services", serviceDescriptors);
            var createOp = generateOperation(CREATE, map, false);
            this.operations.add(createOp);
        }

    }

    @Override
    public DidState getState() {
        return DidState.Builder.create()
                .shortForm(getUriShort())
                .longForm(getUri())
                .operations(operations)
                .build();
    }

    @Override
    public DidOperation getOperation(int index) {
        return operations.get(index);
    }

    @Override
    public String getUri() {
        if (longForm == null) {
            var op = getOperation(0);
            var recoveryKey = op.getRecovery().getPublicKey();
            var updateKey = op.getUpdate().getPublicKey();
            var document = op.getContent();
            longForm = createLongFormDid(recoveryKey, updateKey, document);
        }
        return longForm;
    }

    @Override
    public String getUriShort() {
        String uri = getUri();
        var index = uri.lastIndexOf(":");
        return uri.substring(0, index);
    }

    @Override
    public String getSuffix() {
        String uriShort = getUriShort();
        var index = uriShort.lastIndexOf(":");
        return uriShort.substring(index);
    }

    @Override
    public IonRequest create(Object options) {
        return generateRequest(0, options);
    }

    @Override
    public IonRequest update(Object options) {
        var op = generateOperation(UPDATE, (Map<String, Object>) options, true);
        return generateRequest(operations.indexOf(op), options);
    }

    @Override
    public IonRequest recover(Object options) {
        var op = generateOperation(RECOVER, (Map<String, Object>) options, true);
        return generateRequest(operations.indexOf(op), options);
    }

    @Override
    public IonRequest deactivate() {
        var op = generateOperation(DEACTIVATE, null, true);
        return generateRequest(operations.indexOf(op), null);
    }

    public IonRequest generateRequest(int operationIndex, Object options) {
        var op = getOperation(operationIndex);
        IonRequest request;
        switch (op.type()) {
            case UPDATE:
                request = IonRequestFactory.createUpdateRequest();
                break;
            case RECOVER:
                request = IonRequestFactory.createRecoverRequest();
                break;
            case DEACTIVATE:
                request = IonRequestFactory.createDeactivateRequest();
                break;
            default:
            case CREATE:
                request = IonRequestFactory.createCreateRequest(op.getRecovery().getPublicKey(), op.getUpdate().getPublicKey(), op.getContent());
                break;
        }
        return request;
    }

    private String createLongFormDid(JWK recoveryKey, JWK updateKey, Map<String, Object> document) {

        IonRequest createRequest = IonRequestFactory.createCreateRequest(recoveryKey, updateKey, document);

        var didUniqueSuffix = computeDidUniqueSuffix(createRequest.getSuffixData());

        var shortFormDid = isMainNet() ? "did:ion:" + didUniqueSuffix : "did:ion:" + network + ":" + didUniqueSuffix;

        var initialState = new IonState(createRequest.getSuffixData(), createRequest.getDelta());

        byte[] jsonBytes = JsonCanonicalizer.canonicalizeAsBytes(initialState);
        var base64 = Base64.getUrlEncoder().withoutPadding().encode(jsonBytes);
        var base64String = new String(base64);
        return shortFormDid + ":" + base64String;
    }

    private String computeDidUniqueSuffix(SuffixData suffixData) {

        var bytes = JsonCanonicalizer.canonicalizeAsBytes(suffixData);
        byte[] multihash = MultihashHelper.hash(bytes);

        return new String(Base64.getUrlEncoder().withoutPadding().encode(multihash));

    }

    private boolean isMainNet() {
        return network == null || network.equals("mainnet");
    }

    private DidOperation generateOperation(DidOperationType type, Map<String, Object> content, boolean autoCommit) {
        var ops = operations;
        if (!ops.isEmpty()) {
            var lastOp = ops.get(ops.size() - 1);

            if (lastOp.isDeactivated()) {
                throw new IllegalStateException("Cannot perform further operations on a deactivated DID");
            }
        }
        var op = new DidOperation(type, content);

        if (type != CREATE) {
            Optional<DidOperation> reduce = ops.stream().reduce((last, o) -> o.type() == type || (o.type() == RECOVER && (type == DEACTIVATE || type == UPDATE)) ? o : last);
            reduce.ifPresent(op::setPrevious);
        }

        if (type == CREATE || type == RECOVER) {
            op.setRecovery(KeyPairFactory.generateKeyPair());
        }
        if (type != DEACTIVATE) {
            op.setUpdate(KeyPairFactory.generateKeyPair());

        }
        if (autoCommit) {
            operations.add(op);
        }

        return op;
    }
}
