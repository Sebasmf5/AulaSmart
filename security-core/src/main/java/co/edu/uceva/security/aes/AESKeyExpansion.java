package co.edu.uceva.security.aes;

import co.edu.uceva.security.aes.transformations.SubBytes;

public class AESKeyExpansion {

    private static final int Nk = 4;   // Número de palabras en la clave (AES-128)
    private static final int Nb = 4;   // Número de columnas en el state
    private static final int Nr = 10;  // Número de rondas AES-128

    // Rcon: constantes para KeyExpansion
    private static final byte[] RCON = {
            0x01, 0x02, 0x04, 0x08,
            0x10, 0x20, 0x40, (byte)0x80,
            0x1B, 0x36
    };

    /**
     * Genera todas las subclaves (round keys) a partir de la clave original de 16 bytes
     * @param key clave original de 16 bytes
     * @return subclaves como un arreglo de 11 subclaves de 4x4 bytes
     */
    public static byte[][][] expandKey(byte[] key) {
        if (key.length != 16)
            throw new IllegalArgumentException("La clave debe tener 16 bytes para AES-128");

        int totalWords = Nb * (Nr + 1); // 44 palabras de 4 bytes cada una
        byte[][] w = new byte[totalWords][4];

        // Copiar la clave original como las primeras Nk palabras
        for (int i = 0; i < Nk; i++) {
            w[i][0] = key[4*i];
            w[i][1] = key[4*i + 1];
            w[i][2] = key[4*i + 2];
            w[i][3] = key[4*i + 3];
        }

        // Generar el resto de palabras
        for (int i = Nk; i < totalWords; i++) {
            byte[] temp = w[i - 1].clone();

            if (i % Nk == 0) {
                temp = subWord(rotWord(temp));
                temp[0] ^= RCON[i / Nk - 1];
            }

            // XOR con la palabra Nk posiciones atrás
            for (int j = 0; j < 4; j++) {
                w[i][j] = (byte)(w[i - Nk][j] ^ temp[j]);
            }
        }

        // Convertir palabras a subclaves 4x4 bytes por ronda
        byte[][][] roundKeys = new byte[Nr + 1][4][4];
        for (int round = 0; round <= Nr; round++) {
            for (int col = 0; col < 4; col++) {
                roundKeys[round][0][col] = w[round * 4 + col][0];
                roundKeys[round][1][col] = w[round * 4 + col][1];
                roundKeys[round][2][col] = w[round * 4 + col][2];
                roundKeys[round][3][col] = w[round * 4 + col][3];
            }
        }

        return roundKeys;
    }

    // Rota la palabra 1 byte a la izquierda
    private static byte[] rotWord(byte[] word) {
        return new byte[]{ word[1], word[2], word[3], word[0] };
    }

    // Aplica SubBytes a cada byte de la palabra
    private static byte[] subWord(byte[] word) {
        byte[] result = new byte[4];
        for (int i = 0; i < 4; i++) {
            int val = word[i] & 0xFF;
            result[i] = (byte) SubBytes.getSBoxValue(val);
        }
        return result;
    }
}