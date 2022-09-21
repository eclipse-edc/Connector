/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.azure.cosmos.dialect;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;


/**
 * In Cosmos there are some reserved keywords that cannot be used in projections or filter condition e.g. {@code obj.value}.
 * This collector join a list of strings taking the account the reserved words in input. The reserved word will not be joined
 * with a dot, but they will be encapsulated in a quoted property operator [\"\"]
 */
public class QuotedPathCollector implements Collector<CharSequence, StringBuilder, String> {

    private final List<String> reservedWords;
    private CharSequence lastChar;

    QuotedPathCollector(List<String> reservedWords) {
        this.reservedWords = reservedWords;
    }

    public static QuotedPathCollector quoteJoining(List<String> reservedWords) {
        return new QuotedPathCollector(reservedWords);
    }

    @Override
    public Supplier<StringBuilder> supplier() {
        return StringBuilder::new;
    }

    @Override
    public BiConsumer<StringBuilder, CharSequence> accumulator() {
        return (stringBuilder, charSequence) -> {
            var isReservedKeyword = reservedWords.contains(charSequence.toString());
            if (lastChar != null && !isReservedKeyword) {
                stringBuilder.append(".");
            }
            if (isReservedKeyword) {
                stringBuilder.append(String.format("[\"%s\"]", charSequence));

            } else {
                stringBuilder.append(charSequence);
            }
            lastChar = charSequence;
        };
    }

    @Override
    public BinaryOperator<StringBuilder> combiner() {
        return (stringBuilder, stringBuilder2) -> {
            stringBuilder.append(stringBuilder2);
            return stringBuilder;
        };
    }

    @Override
    public Function<StringBuilder, String> finisher() {
        return StringBuilder::toString;
    }


    @Override
    public Set<Characteristics> characteristics() {
        return Set.of(Characteristics.UNORDERED);
    }
}
