package co.edu.uceva.security.aes;

public class AES {

    private final AESBlockCipher cipher;

    public AES(byte[] key) {
        this.cipher = new AESBlockCipher(key);
    }

    public byte[] encrypt(byte[] plaintext) {

        byte[] padded = AESPadding.applyPadding(plaintext);

        byte[] result = new byte[padded.length];

        for (int i = 0; i < padded.length; i += 16) {

            byte[] block = new byte[16];

            System.arraycopy(padded, i, block, 0, 16);

            byte[] encryptedBlock = cipher.encryptBlock(block);

            System.arraycopy(encryptedBlock, 0, result, i, 16);
        }

        return result;
    }
}