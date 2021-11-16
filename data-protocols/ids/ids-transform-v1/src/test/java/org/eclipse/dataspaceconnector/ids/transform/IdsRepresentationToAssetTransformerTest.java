/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.transform;

import de.fraunhofer.iais.eis.Artifact;
import de.fraunhofer.iais.eis.ArtifactBuilder;
import de.fraunhofer.iais.eis.CustomMediaTypeBuilder;
import de.fraunhofer.iais.eis.Representation;
import de.fraunhofer.iais.eis.RepresentationBuilder;
import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformKeys;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;

public class IdsRepresentationToAssetTransformerTest {
    private static final String ASSET_ID = "1";
    private static final URI REPRESENTATION_URI = URI.create("urn:representation:1");
    private static final String ASSET_FILENAME = "test_filename";
    private static final String ASSET_FILE_EXTENSION = "txt";
    private static final BigInteger ASSET_BYTESIZE = BigInteger.valueOf(5);

    // subject
    private IdsRepresentationToAssetTransformer idsRepresentationToAssetTransformer;

    // mocks
    private Representation representation;
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        idsRepresentationToAssetTransformer = new IdsRepresentationToAssetTransformer();
        Artifact artifact = new ArtifactBuilder()._byteSize_(ASSET_BYTESIZE)._fileName_(ASSET_FILENAME).build();

        representation = new RepresentationBuilder(REPRESENTATION_URI)
                ._instance_(new ArrayList<>(Collections.singletonList(artifact)))
                ._mediaType_(new CustomMediaTypeBuilder()._filenameExtension_(ASSET_FILE_EXTENSION).build())
                .build();
        context = EasyMock.createMock(TransformerContext.class);
    }

    @Test
    void testThrowsNullPointerExceptionForAll() {
        EasyMock.replay(context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            idsRepresentationToAssetTransformer.transform(null, null);
        });
    }

    @Test
    void testThrowsNullPointerExceptionForContext() {
        EasyMock.replay(context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            idsRepresentationToAssetTransformer.transform(representation, null);
        });
    }

    @Test
    void testReturnsNull() {
        EasyMock.replay(context);

        var result = idsRepresentationToAssetTransformer.transform(null, context);

        Assertions.assertNull(result);
    }

    @Test
    void testSuccessfulMap() {
        // record
        EasyMock.replay(context);

        // invoke
        var result = idsRepresentationToAssetTransformer.transform(representation, context);

        // verify
        Assertions.assertNotNull(result);
        Assertions.assertEquals(ASSET_ID, result.getId());
        Assertions.assertEquals(result.getProperties().get(TransformKeys.KEY_ASSET_BYTE_SIZE), ASSET_BYTESIZE);
        Assertions.assertEquals(result.getProperties().get(TransformKeys.KEY_ASSET_FILE_NAME), ASSET_FILENAME);
        Assertions.assertEquals(result.getProperties().get(TransformKeys.KEY_ASSET_FILE_EXTENSION), ASSET_FILE_EXTENSION);
    }

    @AfterEach
    void tearDown() {
        EasyMock.verify(context);
    }

}
