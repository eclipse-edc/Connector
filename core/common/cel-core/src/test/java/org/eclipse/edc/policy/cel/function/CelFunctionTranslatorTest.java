/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.policy.cel.function;

import dev.cel.common.CelFunctionDecl;
import dev.cel.common.CelOverloadDecl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CelFunctionTranslatorTest {

    /**
     * CEL requires every overload of a function name to be declared together, so the translator must group them.
     */
    @Test
    void toDeclarations_groupsOverloadsBySharedName() {
        var functions = List.of(
                function("hasClaim", "has_claim_1", CelValueType.BOOL, List.of(CelValueType.LIST, CelValueType.STRING)),
                function("hasClaim", "has_claim_2", CelValueType.BOOL, List.of(CelValueType.LIST, CelValueType.STRING, CelValueType.DYN)),
                function("withType", "with_type", CelValueType.LIST, List.of(CelValueType.LIST, CelValueType.STRING)));

        var declarations = CelFunctionTranslator.toDeclarations(functions);

        assertThat(declarations).hasSize(2);
        assertThat(declarations).extracting(CelFunctionDecl::name).containsExactly("hasClaim", "withType");
        assertThat(declarations.get(0).overloads()).extracting(CelOverloadDecl::overloadId)
                .containsExactlyInAnyOrder("has_claim_1", "has_claim_2");
        assertThat(declarations.get(0).overloads()).allMatch(CelOverloadDecl::isInstanceFunction);
    }

    @Test
    void toDeclarations_marksGlobalFunctions() {
        var global = new CelFunction("hasCredential", "global_has_credential", false, CelValueType.BOOL,
                List.of(CelValueType.LIST, CelValueType.STRING), args -> true);

        var declarations = CelFunctionTranslator.toDeclarations(List.of(global));

        assertThat(declarations).singleElement()
                .satisfies(decl -> assertThat(decl.overloads()).singleElement()
                        .matches(overload -> !overload.isInstanceFunction()));
    }

    @Test
    void toBindings_emitsOneBindingPerOverload() {
        var functions = List.of(
                function("hasClaim", "has_claim_1", CelValueType.BOOL, List.of(CelValueType.LIST, CelValueType.STRING)),
                function("hasClaim", "has_claim_2", CelValueType.BOOL, List.of(CelValueType.LIST, CelValueType.STRING, CelValueType.DYN)));

        var bindings = CelFunctionTranslator.toBindings(functions);

        assertThat(bindings).hasSize(2).extracting(binding -> binding.getOverloadId())
                .containsExactly("has_claim_1", "has_claim_2");
        assertThat(bindings.get(0).getArgTypes()).containsExactly(List.class, String.class);
        assertThat(bindings.get(1).getArgTypes()).containsExactly(List.class, String.class, Object.class);
    }

    @Test
    void toBindings_passesArgumentsInDeclarationOrder() throws Exception {
        var function = new CelFunction("concat", "concat", true, CelValueType.STRING,
                List.of(CelValueType.STRING, CelValueType.STRING), args -> args.get(0) + "-" + args.get(1));

        var binding = CelFunctionTranslator.toBindings(List.of(function)).get(0);

        assertThat(binding.getDefinition().apply(new Object[]{ "a", "b" })).isEqualTo("a-b");
    }

    private CelFunction function(String name, String overloadId, CelValueType resultType, List<CelValueType> argumentTypes) {
        return new CelFunction(name, overloadId, true, resultType, argumentTypes, args -> null);
    }
}
