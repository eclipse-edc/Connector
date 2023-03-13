package org.eclipse.edc.jsonld.transformer;

import java.util.Set;

/**
 * JSON-LD keywords as defined by {@see https://www.w3.org/TR/json-ld/#syntax-tokens-and-keywords}.
 */
public interface JsonLdKeywords {

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
