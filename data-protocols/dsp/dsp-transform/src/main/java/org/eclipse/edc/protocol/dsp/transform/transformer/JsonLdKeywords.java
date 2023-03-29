/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.transform.transformer;

import java.util.Set;

public interface JsonLdKeywords {
    
    //TODO use class from json-ld module
    
    String BASE = "@base";
    String CONTAINER = "@container";
    String CONTEXT = "@context";
    String DIRECTION = "@direction";
    String GRAPH = "@graph";
    String ID = "@id";
    String IMPORT = "@import";
    String INCLUDED = "@included";
    String INDEX = "@index";
    String JSON = "@json";
    String LANGUAGE = "@language";
    String LIST = "@list";
    String NEST = "@nest";
    String NODE = "@node";
    String PREFIX = "@prefix";
    String PROPAGATE = "@propagate";
    String PROTECTED = "@protected";
    String REVERSE = "@reverse";
    String SET = "@set";
    String TYPE = "@type";
    String VALUE = "@value";
    String VERSION = "@version";
    String VOCAB = "@vocab";
    
    Set<String> KEYWORDS = Set.of(
            BASE,
            CONTAINER,
            CONTEXT,
            DIRECTION,
            GRAPH,
            ID,
            IMPORT,
            INCLUDED,
            INDEX,
            JSON,
            LANGUAGE,
            LIST,
            NEST,
            NODE,
            PREFIX,
            PROPAGATE,
            PROTECTED,
            REVERSE,
            SET,
            TYPE,
            VALUE,
            VERSION,
            VOCAB);
    
}
