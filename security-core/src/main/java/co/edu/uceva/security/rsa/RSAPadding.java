package co.edu.uceva.security.rsa;

import java.security.SecureRandom;

public class RSAPadding {

    private static final SecureRandom random = new SecureRandom();

    /**
     * Aplica padding tipo PKCS#1 simplificado
     * Estructura:
     * 0x00 | 0x02 | random bytes | 0x00 | mensaje
     */
    public static byte[] addPadding(byte[] message, int blockSize) {

        if (message.length > blockSize - 11) {
            throw new IllegalArgumentException("Mensaje demasiado largo para el bloque RSA");
        }

        byte[] padded = new byte[blockSize];

        padded[0] = 0x00;
        padded[1] = 0x02;

        int paddingLength = blockSize - message.length - 3;

        for (int i = 0; i < paddingLength; i++) {
            byte b = 0;
            while (b == 0) {
                b = (byte) random.nextInt(256);
            }
            padded[2 + i] = b;
        }

        padded[2 + paddingLength] = 0x00;

        System.arraycopy(message, 0, padded, 3 + paddingLength, message.length);

        return padded;
    }

    /**
     * Elimina el padding
     */
    public static byte[] removePadding(byte[] padded) {

        if (padded[0] != 0x00 || padded[1] != 0x02) {
            throw new RuntimeException("Padding inválido");
        }

        int i = 2;

        while (padded[i] != 0x00) {
            i++;
        }

        i++;

        byte[] message = new byte[padded.length - i];
        System.arraycopy(padded, i, message, 0, message.length);

        return message;
    }
}