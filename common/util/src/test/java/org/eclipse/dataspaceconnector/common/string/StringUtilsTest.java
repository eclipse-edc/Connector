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

package org.eclipse.dataspaceconnector.common.string;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.in;

class StringUtilsTest {

    @Test
    void testEquals() {
        assertThat(StringUtils.equals("", "")).isTrue();
        assertThat(StringUtils.equals("", null)).isFalse();
        assertThat(StringUtils.equals("", " ")).isFalse();
        assertThat(StringUtils.equals("foo", "fOo")).isFalse();
        assertThat(StringUtils.equals("foo", "fo o")).isFalse();
        assertThat(StringUtils.equals("foo", "Foo")).isFalse();
        assertThat(StringUtils.equals(null, "")).isFalse();
        assertThat(StringUtils.equals(null, null)).isTrue();
    }

    @Test
    void isNullOrEmpty() {
        assertThat(StringUtils.isNullOrEmpty("")).isTrue();
        assertThat(StringUtils.isNullOrEmpty("  ")).isFalse();
        assertThat(StringUtils.isNullOrEmpty(null)).isTrue();
        assertThat(StringUtils.isNullOrEmpty("foobar")).isFalse();
    }

    @Test
    void isNullOrBlank() {
        assertThat(StringUtils.isNullOrBlank("")).isTrue();
        assertThat(StringUtils.isNullOrBlank("  ")).isTrue();
        assertThat(StringUtils.isNullOrBlank(null)).isTrue();
        assertThat(StringUtils.isNullOrBlank("foobar")).isFalse();
    }

    @Test
    void equalsIgnoreCase() {
        assertThat(StringUtils.equalsIgnoreCase("", "")).isTrue();
        assertThat(StringUtils.equalsIgnoreCase("", null)).isFalse();
        assertThat(StringUtils.equalsIgnoreCase("", " ")).isFalse();
        assertThat(StringUtils.equalsIgnoreCase("foo", "fOo")).isTrue();
        assertThat(StringUtils.equalsIgnoreCase("foo", "fo o")).isFalse();
        assertThat(StringUtils.equalsIgnoreCase("foo", "Foo")).isTrue();
        assertThat(StringUtils.equalsIgnoreCase(null, "")).isFalse();
        assertThat(StringUtils.equalsIgnoreCase(null, null)).isTrue();
        assertThat(StringUtils.equalsIgnoreCase("FOO", "fOo")).isTrue();

    }

    @Test
    void encodeToHexBytes() {

        final var input = "cd18bb83b12080fae7945d31688f62ad968a7e980feb22642729d104e3097fe2";
        final var expectedOutput = new byte[]{
                (byte) 205,
                24,
                (byte) 187,
                (byte) 131,
                (byte) 177,
                32,
                (byte) 128,
                (byte) 250,
                (byte) 231,
                (byte) 148,
                93,
                49,
                104,
                (byte) 143,
                98,
                (byte) 173,
                (byte) 150,
                (byte) 138,
                (byte) 126,
                (byte) 152,
                15,
                (byte) 235,
                34,
                100,
                39,
                41,
                (byte) 209,
                4,
                (byte) 227,
                9,
                127,
                (byte) 226

        };

        var encoded = StringUtils.encodeToHexBytes(input);
        assertThat(encoded).isEqualTo(expectedOutput);

    }

    @Test
    void encodeToHex() {
        var input = "64393736656431363232396564653032653263343334336637336561313363326536323363336330376537613866346362313165376535633635643732303339306266313838313736393339353034306538653063383763396435383139643431333838356630346535636430343564343334633563626538313133353364616662386138343838633934343761383532613963353764383231353334636262373434333166613739373437323634386534336464376638643939383232623737323037343434313864303838356532633763313666346338626331656636383662393533316162613330353065656335616664363761633762623932383736316362353431323861646363643163303234356632613761313331363662646262303631363830633635383430393133303466366135313836643331313739396466316264336262656364633536343662313864333333";
        var expectedOutput = "d976ed16229ede02e2c4343f73ea13c2e623c3c07e7a8f4cb11e7e5c65d720390bf1881769395040e8e0c87c9d5819d413885f04e5cd045d434c5cbe811353dafb8a8488c9447a852a9c57d821534cbb74431fa797472648e43dd7f8d99822b7720744418d0885e2c7c16f4c8bc1ef686b9531aba3050eec5afd67ac7bb928761cb54128adccd1c0245f2a7a13166bdbb061680c6584091304f6a5186d311799df1bd3bbecdc5646b18d333";

        assertThat(StringUtils.encodeToHex(input)).isEqualTo(expectedOutput);
    }

    @Test
    void toStringTest() { //cannot be named "toString()"
        assertThat(StringUtils.toString("")).isEqualTo("");
        assertThat(StringUtils.toString(23)).isEqualTo("23");
        assertThat(StringUtils.toString(null)).isEqualTo(null);
        assertThat(StringUtils.toString(new Object())).contains("java.lang.Object@");
    }
}
