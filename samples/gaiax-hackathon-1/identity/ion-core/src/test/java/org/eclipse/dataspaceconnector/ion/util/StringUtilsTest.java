package org.eclipse.dataspaceconnector.ion.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 */
class StringUtilsTest {
    @Test
    void encodeToHexBytes() {

        final var input = "cd18bb83b12080fae7945d31688f62ad968a7e980feb22642729d104e3097fe2";
        var expectedOutput = new byte[]{
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

        var encoded = HexStringUtils.encodeToHexBytes(input);
        Assertions.assertThat(encoded).isEqualTo(expectedOutput);

    }

    @Test
    void encodeToHex() {
        var input = "64393736656431363232396564653032653263343334336637336561313363326536323363336330376537613866346362313165376535633635643732303339306266313838313736393339353034306538653063383763396435383139643431333838356630346535636430343564343334633563626538313133353364616662386138343838633934343761383532613963353764383231353334636262373434333166613739373437323634386534336464376638643939383232623737323037343434313864303838356532633763313666346338626331656636383662393533316162613330353065656335616664363761633762623932383736316362353431323861646363643163303234356632613761313331363662646262303631363830633635383430393133303466366135313836643331313739396466316264336262656364633536343662313864333333";
        var expectedOutput = "d976ed16229ede02e2c4343f73ea13c2e623c3c07e7a8f4cb11e7e5c65d720390bf1881769395040e8e0c87c9d5819d413885f04e5cd045d434c5cbe811353dafb8a8488c9447a852a9c57d821534cbb74431fa797472648e43dd7f8d99822b7720744418d0885e2c7c16f4c8bc1ef686b9531aba3050eec5afd67ac7bb928761cb54128adccd1c0245f2a7a13166bdbb061680c6584091304f6a5186d311799df1bd3bbecdc5646b18d333";

        Assertions.assertThat(HexStringUtils.encodeToHex(input)).isEqualTo(expectedOutput);
    }
}
