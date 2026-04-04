package co.edu.uceva.security.aes;

public class AES {

    private final AESBlockCipher cipher;
    private final AESBlockDecipher decipher;

    public AES(byte[] key) {
        this.cipher = new AESBlockCipher(key);
        this.decipher = new AESBlockDecipher(key);
    }

    // Cifrado
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

    // Descifrado
    public byte[] decrypt(byte[] ciphertext) {
        if (ciphertext.length % 16 != 0) {
            throw new IllegalArgumentException("Ciphertext length must be multiple of 16 bytes.");
        }

        byte[] result = new byte[ciphertext.length];

        for (int i = 0; i < ciphertext.length; i += 16) {
            byte[] block = new byte[16];
            System.arraycopy(ciphertext, i, block, 0, 16);
            byte[] decryptedBlock = decipher.decryptBlock(block);
            System.arraycopy(decryptedBlock, 0, result, i, 16);
        }

        // Remover padding al final
        return AESPadding.removePadding(result);
    }
}