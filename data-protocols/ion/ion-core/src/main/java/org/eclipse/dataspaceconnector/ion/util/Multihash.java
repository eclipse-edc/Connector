package org.eclipse.dataspaceconnector.ion.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


/**
 * MIT License
 * <p>
 * Copyright (c) 2015 Ian Preston
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * <p>
 * <p>
 * Code copied from:
 * https://github.com/multiformats/java-multihash/blob/master/src/main/java/io/ipfs/multihash/Multihash.java
 * <p>
 * <p>
 * Date copied: Sept 16, 2021
 */
public class Multihash {
    public static final int MAX_IDENTITY_HASH_LENGTH = 1024 * 1024;
    private final Type type;
    private final byte[] hash;

    public Multihash(Type type, byte[] hash) {
        if (hash.length > 127 && type != Type.id) {
            throw new IllegalStateException("Unsupported hash size: " + hash.length);
        }
        if (hash.length > MAX_IDENTITY_HASH_LENGTH) {
            throw new IllegalStateException("Unsupported hash size: " + hash.length);
        }
        if (hash.length != type.length && type != Type.id) {
            throw new IllegalStateException("Incorrect hash length: " + hash.length + " != " + type.length);
        }
        this.type = type;
        this.hash = hash;
    }

    public Multihash(Multihash toClone) {
        this(toClone.type, toClone.hash); // N.B. despite being a byte[], hash is immutable
    }

    public static Multihash deserialize(InputStream din) throws IOException {
        int type = (int) readVarint(din);
        int len = (int) readVarint(din);
        Type t = Type.lookup(type);
        byte[] hash = new byte[len];
        int total = 0;
        while (total < len) {
            int read = din.read(hash);
            if (read < 0) {
                throw new EOFException();
            } else {
                total += read;
            }
        }
        return new Multihash(t, hash);
    }

    public static Multihash deserialize(byte[] raw) throws IOException {
        return deserialize(new ByteArrayInputStream(raw));
    }

    public static Multihash fromHex(String hex) {
        if (hex.length() % 2 != 0) {
            throw new IllegalStateException("Odd number of hex digits!");
        }

        try (ByteArrayOutputStream bout = new ByteArrayOutputStream()) {
            for (int i = 0; i < hex.length() - 1; i += 2) {
                bout.write(Integer.valueOf(hex.substring(i, i + 2), 16));
            }
            return Multihash.deserialize(bout.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to handle Multihash conversion to Hex properly");
        }
    }

    public static Multihash fromBase58(String base58) {
        try {
            return Multihash.deserialize(Base58.decode(base58));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static long readVarint(InputStream in) throws IOException {
        long x = 0;
        int s = 0;
        for (int i = 0; i < 10; i++) {
            int b = in.read();
            if (b < 0x80) {
                if (i == 9 && b > 1) {
                    throw new IllegalStateException("Overflow reading varint!");
                }
                return x | (((long) b) << s);
            }
            x |= ((long) b & 0x7f) << s;
            s += 7;
        }
        throw new IllegalStateException("Varint too long!");
    }

    public static void putUvarint(OutputStream out, long x) throws IOException {
        while (x >= 0x80) {
            out.write((byte) (x | 0x80));
            x >>= 7;
        }
        out.write((byte) x);
    }

    public byte[] toBytes() {
        try {
            ByteArrayOutputStream res = new ByteArrayOutputStream();
            putUvarint(res, type.index);
            putUvarint(res, hash.length);
            res.write(hash);
            return res.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Type getType() {
        return type;
    }

    public byte[] getHash() {
        return Arrays.copyOf(hash, hash.length);
    }

    public void serialize(OutputStream out) {
        try {
            putUvarint(out, type.index);
            putUvarint(out, hash.length);
            out.write(hash);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(hash) ^ type.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Multihash)) {
            return false;
        }
        return type == ((Multihash) o).type && Arrays.equals(hash, ((Multihash) o).hash);
    }

    @Override
    public String toString() {
        return toBase58();
    }

    public String toHex() {
        return Base16.encode(toBytes());
    }

    public String toBase58() {
        return Base58.encode(toBytes());
    }

    public enum Type {
        id(0, -1),
        md5(0xd5, 16),
        sha1(0x11, 20),
        sha2_256(0x12, 32),
        sha2_512(0x13, 64),
        dbl_sha2_256(0x56, 32),
        sha3_224(0x17, 24),
        sha3_256(0x16, 32),
        sha3_512(0x14, 64),
        shake_128(0x18, 32),
        shake_256(0x19, 64),
        keccak_224(0x1a, 24),
        keccak_256(0x1b, 32),
        keccak_384(0x1c, 48),
        keccak_512(0x1d, 64),
        murmur3(0x22, 4),

        // blake2b (64 codes)
        blake2b_8(0xb201, 1),
        blake2b_16(0xb202, 2),
        blake2b_24(0xb203, 3),
        blake2b_32(0xb204, 4),
        blake2b_40(0xb205, 5),
        blake2b_48(0xb206, 6),
        blake2b_56(0xb207, 7),
        blake2b_64(0xb208, 8),
        blake2b_72(0xb209, 9),
        blake2b_80(0xb20a, 10),
        blake2b_88(0xb20b, 11),
        blake2b_96(0xb20c, 12),
        blake2b_104(0xb20d, 13),
        blake2b_112(0xb20e, 14),
        blake2b_120(0xb20f, 15),
        blake2b_128(0xb210, 16),
        blake2b_136(0xb211, 17),
        blake2b_144(0xb212, 18),
        blake2b_152(0xb213, 19),
        blake2b_160(0xb214, 20),
        blake2b_168(0xb215, 21),
        blake2b_176(0xb216, 22),
        blake2b_184(0xb217, 23),
        blake2b_192(0xb218, 24),
        blake2b_200(0xb219, 25),
        blake2b_208(0xb21a, 26),
        blake2b_216(0xb21b, 27),
        blake2b_224(0xb21c, 28),
        blake2b_232(0xb21d, 29),
        blake2b_240(0xb21e, 30),
        blake2b_248(0xb21f, 31),
        blake2b_256(0xb220, 32),
        blake2b_264(0xb221, 33),
        blake2b_272(0xb222, 34),
        blake2b_280(0xb223, 35),
        blake2b_288(0xb224, 36),
        blake2b_296(0xb225, 37),
        blake2b_304(0xb226, 38),
        blake2b_312(0xb227, 39),
        blake2b_320(0xb228, 40),
        blake2b_328(0xb229, 41),
        blake2b_336(0xb22a, 42),
        blake2b_344(0xb22b, 43),
        blake2b_352(0xb22c, 44),
        blake2b_360(0xb22d, 45),
        blake2b_368(0xb22e, 46),
        blake2b_376(0xb22f, 47),
        blake2b_384(0xb230, 48),
        blake2b_392(0xb231, 49),
        blake2b_400(0xb232, 50),
        blake2b_408(0xb233, 51),
        blake2b_416(0xb234, 52),
        blake2b_424(0xb235, 53),
        blake2b_432(0xb236, 54),
        blake2b_440(0xb237, 55),
        blake2b_448(0xb238, 56),
        blake2b_456(0xb239, 57),
        blake2b_464(0xb23a, 58),
        blake2b_472(0xb23b, 59),
        blake2b_480(0xb23c, 60),
        blake2b_488(0xb23d, 61),
        blake2b_496(0xb23e, 62),
        blake2b_504(0xb23f, 63),
        blake2b_512(0xb240, 64),

        // blake2s (32 codes)
        blake2s_8(0xb241, 1),
        blake2s_16(0xb242, 2),
        blake2s_24(0xb243, 3),
        blake2s_32(0xb244, 4),
        blake2s_40(0xb245, 5),
        blake2s_48(0xb246, 6),
        blake2s_56(0xb247, 7),
        blake2s_64(0xb248, 8),
        blake2s_72(0xb249, 9),
        blake2s_80(0xb24a, 10),
        blake2s_88(0xb24b, 11),
        blake2s_96(0xb24c, 12),
        blake2s_104(0xb24d, 13),
        blake2s_112(0xb24e, 14),
        blake2s_120(0xb24f, 15),
        blake2s_128(0xb250, 16),
        blake2s_136(0xb251, 17),
        blake2s_144(0xb252, 18),
        blake2s_152(0xb253, 19),
        blake2s_160(0xb254, 20),
        blake2s_168(0xb255, 21),
        blake2s_176(0xb256, 22),
        blake2s_184(0xb257, 23),
        blake2s_192(0xb258, 24),
        blake2s_200(0xb259, 25),
        blake2s_208(0xb25a, 26),
        blake2s_216(0xb25b, 27),
        blake2s_224(0xb25c, 28),
        blake2s_232(0xb25d, 29),
        blake2s_240(0xb25e, 30),
        blake2s_248(0xb25f, 31),
        blake2s_256(0xb260, 32);

        private static final Map<Integer, Type> lookup = new HashMap<>();

        static {
            for (Type t : Type.values()) {
                lookup.put(t.index, t);
            }
        }

        public final int index, length;

        Type(int index, int length) {
            this.index = index;
            this.length = length;
        }

        public static Type lookup(int t) {
            Type type = lookup.get(t);
            if (type == null) {
                throw new IllegalStateException(String.format("Unknown Multihash type: 0x%x", t));
            }
            return type;
        }

    }
}
