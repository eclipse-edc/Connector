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

package org.eclipse.edc.protocol.ids.transform;

import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.protocol.ids.serialization.IdsConstraintBuilder;
import org.eclipse.edc.protocol.ids.transform.type.policy.ProhibitionFromIdsProhibitionTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProhibitionFromIdsProhibitionTransformerTest {

    private static final String TARGET = "https://target.com";
    private static final String ACTION = "USE";
    private static final URI TARGET_URI = URI.create(TARGET);
    private static final String ASSIGNER = "https://assigner.com";
    private static final URI ASSIGNER_URI = URI.create(ASSIGNER);
    private static final String ASSIGNEE = "https://assignee.com";
    private static final URI ASSIGNEE_URI = URI.create(ASSIGNEE);

    // subject
    private ProhibitionFromIdsProhibitionTransformer transformer;

    // mocks
    private de.fraunhofer.iais.eis.Prohibition idsPermission;
    private de.fraunhofer.iais.eis.Constraint idsConstraint;
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new ProhibitionFromIdsProhibitionTransformer();
        idsConstraint = new IdsConstraintBuilder().build();
        idsPermission = new de.fraunhofer.iais.eis.ProhibitionBuilder()
                ._action_(new ArrayList<>(Collections.singletonList(de.fraunhofer.iais.eis.Action.USE)))
                ._target_(TARGET_URI)
                ._constraint_(new ArrayList<>(Collections.singletonList(idsConstraint)))
                ._assignee_(new ArrayList<>(Collections.singletonList(ASSIGNEE_URI)))
                ._assigner_(new ArrayList<>(Collections.singletonList(ASSIGNER_URI)))
                .build();
        context = mock(TransformerContext.class);
    }

    @Test
    void testSuccessfulSimple() {
        Constraint edcConstraint = mock(Constraint.class);

        when(context.transform(eq(idsConstraint), eq(Constraint.class))).thenReturn(edcConstraint);

        var result = transformer.transform(idsPermission, context);

        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getAction());
        Assertions.assertNotNull(result.getConstraints());
        Assertions.assertEquals(ACTION, result.getAction().getType());
        Assertions.assertEquals(TARGET, result.getTarget());
        Assertions.assertEquals(ASSIGNER, result.getAssigner());
        Assertions.assertEquals(ASSIGNEE, result.getAssignee());
        Assertions.assertEquals(1, result.getConstraints().size());
        Assertions.assertEquals(edcConstraint, result.getConstraints().get(0));
        verify(context, times(1)).transform(any(), any());
    }

}
