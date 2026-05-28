package co.edu.uceva.security.aes;

public class AESPadding {

    private static final int BLOCK_SIZE = 16;

    public static byte[] applyPadding(byte[] data) {

        int paddingLength = BLOCK_SIZE - (data.length % BLOCK_SIZE);

        byte[] padded = new byte[data.length + paddingLength];

        System.arraycopy(data, 0, padded, 0, data.length);

        for (int i = data.length; i < padded.length; i++) {
            padded[i] = (byte) paddingLength;
        }

        return padded;
    }

    public static byte[] removePadding(byte[] padded) {

        int paddingLength = padded[padded.length - 1] & 0xFF;

        if (paddingLength < 1 || paddingLength > BLOCK_SIZE) {
            throw new RuntimeException("Padding inválido");
        }

        for (int i = padded.length - paddingLength; i < padded.length; i++) {
            if (padded[i] != (byte) paddingLength) {
                throw new RuntimeException("Padding inválido");
            }
        }

        byte[] data = new byte[padded.length - paddingLength];
        System.arraycopy(padded, 0, data, 0, data.length);

        return data;
    }
}