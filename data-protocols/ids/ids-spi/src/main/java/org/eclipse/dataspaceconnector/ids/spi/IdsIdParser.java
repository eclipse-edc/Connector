package org.eclipse.dataspaceconnector.ids.spi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ID / URI parser for IDS resources.
 */
public class IdsIdParser {

    public static final String SCHEME = "urn";
    public static final String DELIMITER = ":";

    private static final String IDS_URN_REGEX = "^urn:(?<type>\\w+):(?<id>.+)$";

    public static IdsId parse(String urn) {
        if (urn == null) {
            throw new IllegalArgumentException("urn must not be null");
        }

        Pattern p = Pattern.compile(IDS_URN_REGEX);
        Matcher m = p.matcher(urn);

        if (!m.matches()) {
            throw new IllegalArgumentException("Unexpected scheme");
        }

        IdsType type = IdsType.fromValue(m.group("type"));
        String id = m.group("id");

        return IdsId.Builder.newInstance().type(type).value(id).build();
    }
}
