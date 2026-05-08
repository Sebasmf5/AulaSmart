package co.edu.uceva.security.aes;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * HMAC-SHA256 implementado con SHA-256 puro (sin librerías externas).
 *
 * <p>HMAC(K, m) = H((K XOR opad) || H((K XOR ipad) || m))</p>
 *
 * <p>Se usa para autenticidad e integridad del payload cifrado.
 * Si el HMAC no coincide, el mensaje fue alterado (ataque de manipulación).</p>
 */
public class HmacSHA256 {

    private static final int BLOCK_SIZE = 64; // SHA-256 block size en bytes
    private static final byte IPAD = 0x36;
    private static final byte OPAD = 0x5C;

    /**
     * Calcula el HMAC-SHA256 de un mensaje con la clave dada.
     *
     * @param key     clave HMAC (puede ser cualquier longitud)
     * @param message mensaje a autenticar
     * @return tag HMAC de 32 bytes
     */
    public static byte[] compute(byte[] key, byte[] message) {
        // 1. Si la clave es mayor al block size, hacer hash de ella
        if (key.length > BLOCK_SIZE) {
            key = sha256(key);
        }

        // 2. Pad la clave al block size con ceros
        byte[] kPadded = new byte[BLOCK_SIZE];
        System.arraycopy(key, 0, kPadded, 0, key.length);

        // 3. Calcular ipad y opad XOR con la clave
        byte[] kIpad = new byte[BLOCK_SIZE];
        byte[] kOpad = new byte[BLOCK_SIZE];
        for (int i = 0; i < BLOCK_SIZE; i++) {
            kIpad[i] = (byte) (kPadded[i] ^ IPAD);
            kOpad[i] = (byte) (kPadded[i] ^ OPAD);
        }

        // 4. HMAC = SHA256(kOpad || SHA256(kIpad || message))
        byte[] innerInput = concat(kIpad, message);
        byte[] innerHash  = sha256(innerInput);
        byte[] outerInput = concat(kOpad, innerHash);
        return sha256(outerInput);
    }

    /**
     * Compara dos HMACs en tiempo constante para evitar timing attacks.
     */
    public static boolean verify(byte[] expected, byte[] actual) {
        if (expected.length != actual.length) return false;
        int diff = 0;
        for (int i = 0; i < expected.length; i++) {
            diff |= expected[i] ^ actual[i];
        }
        return diff == 0;
    }

    // -------------------------------------------------------------------------
    // SHA-256 implementación propia
    // -------------------------------------------------------------------------

    private static final int[] K = {
        0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5,
        0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
        0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3,
        0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
        0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc,
        0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7,
        0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
        0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
        0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3,
        0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
        0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208,
        0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
    };

    public static byte[] sha256(byte[] message) {
        // Valores iniciales del hash (primeros 32 bits de las raíces cuadradas de los primeros 8 primos)
        int h0 = 0x6a09e667, h1 = 0xbb67ae85, h2 = 0x3c6ef372, h3 = 0xa54ff53a;
        int h4 = 0x510e527f, h5 = 0x9b05688c, h6 = 0x1f83d9ab, h7 = 0x5be0cd19;

        // Pre-processing: añadir padding
        byte[] padded = padMessage(message);

        // Procesar en bloques de 512 bits (64 bytes)
        for (int i = 0; i < padded.length; i += 64) {
            int[] w = new int[64];

            // Preparar el message schedule
            for (int j = 0; j < 16; j++) {
                w[j] = ((padded[i + j*4] & 0xFF) << 24)
                      | ((padded[i + j*4 + 1] & 0xFF) << 16)
                      | ((padded[i + j*4 + 2] & 0xFF) << 8)
                      |  (padded[i + j*4 + 3] & 0xFF);
            }
            for (int j = 16; j < 64; j++) {
                int s0 = rotRight(w[j-15], 7) ^ rotRight(w[j-15], 18) ^ (w[j-15] >>> 3);
                int s1 = rotRight(w[j-2],  17) ^ rotRight(w[j-2],  19) ^ (w[j-2]  >>> 10);
                w[j] = w[j-16] + s0 + w[j-7] + s1;
            }

            // Compression
            int a = h0, b = h1, c = h2, d = h3, e = h4, f = h5, g = h6, h = h7;
            for (int j = 0; j < 64; j++) {
                int S1   = rotRight(e, 6) ^ rotRight(e, 11) ^ rotRight(e, 25);
                int ch   = (e & f) ^ (~e & g);
                int temp1 = h + S1 + ch + K[j] + w[j];
                int S0   = rotRight(a, 2) ^ rotRight(a, 13) ^ rotRight(a, 22);
                int maj  = (a & b) ^ (a & c) ^ (b & c);
                int temp2 = S0 + maj;

                h = g; g = f; f = e; e = d + temp1;
                d = c; c = b; b = a; a = temp1 + temp2;
            }

            h0 += a; h1 += b; h2 += c; h3 += d;
            h4 += e; h5 += f; h6 += g; h7 += h;
        }

        // Producir el hash final (32 bytes)
        byte[] result = new byte[32];
        writeInt(result, 0, h0); writeInt(result, 4, h1);
        writeInt(result, 8, h2); writeInt(result, 12, h3);
        writeInt(result, 16, h4); writeInt(result, 20, h5);
        writeInt(result, 24, h6); writeInt(result, 28, h7);
        return result;
    }

    private static byte[] padMessage(byte[] msg) {
        int origLen = msg.length;
        int bitLen  = origLen * 8;
        // Longitud padding: añadir 1 bit (0x80), luego ceros hasta que len ≡ 56 mod 64, luego 8 bytes de longitud
        int padLen = (56 - (origLen + 1) % 64 + 64) % 64;
        byte[] padded = new byte[origLen + 1 + padLen + 8];
        System.arraycopy(msg, 0, padded, 0, origLen);
        padded[origLen] = (byte) 0x80;
        // Longitud del mensaje original en bits (big-endian, 64 bits)
        for (int i = 0; i < 8; i++) {
            padded[padded.length - 1 - i] = (byte) (((long) bitLen) >> (8 * i));
        }
        return padded;
    }

    private static int rotRight(int x, int n) { return (x >>> n) | (x << (32 - n)); }
    private static void writeInt(byte[] b, int off, int val) {
        b[off] = (byte)(val >> 24); b[off+1] = (byte)(val >> 16);
        b[off+2] = (byte)(val >> 8); b[off+3] = (byte) val;
    }
    private static byte[] concat(byte[] a, byte[] b) {
        byte[] r = new byte[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }
}
