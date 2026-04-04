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
}