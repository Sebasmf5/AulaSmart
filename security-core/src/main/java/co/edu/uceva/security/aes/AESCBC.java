package co.edu.uceva.security.aes;

import java.security.SecureRandom;

/**
 * AES en modo CBC (Cipher Block Chaining) usando la implementación propia de AESBlockCipher.
 *
 * <p>Modo CBC: cada bloque de plaintext se hace XOR con el bloque cifrado anterior
 * antes de cifrarlo. El primer bloque usa el IV como "bloque previo".</p>
 *
 * <p>Formato del ciphertext devuelto: [IV (16 bytes)] || [ciphertext (N bloques)]</p>
 *
 * Ventajas sobre ECB:
 * - Bloques idénticos producen ciphertext diferente.
 * - El IV aleatorio evita ataques de replay con mismo plaintext.
 */
public class AESCBC {

    private static final int BLOCK_SIZE = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AESBlockCipher cipher;
    private final AESBlockDecipher decipher;

    public AESCBC(byte[] key) {
        if (key.length != 16) {
            throw new IllegalArgumentException("La clave AES debe ser de 16 bytes (AES-128).");
        }
        this.cipher   = new AESBlockCipher(key);
        this.decipher = new AESBlockDecipher(key);
    }

    /**
     * Genera un IV aleatorio de 16 bytes usando SecureRandom.
     * Debe generarse uno nuevo por cada mensaje (nunca reutilizar).
     */
    public static byte[] generateIV() {
        byte[] iv = new byte[BLOCK_SIZE];
        SECURE_RANDOM.nextBytes(iv);
        return iv;
    }

    /**
     * Cifra el plaintext en modo CBC con el IV dado.
     *
     * @param plaintext bytes a cifrar
     * @param iv        vector de inicialización de 16 bytes (aleatorio, nunca reutilizar)
     * @return ciphertext con padding PKCS7 aplicado
     */
    public byte[] encrypt(byte[] plaintext, byte[] iv) {
        if (iv.length != BLOCK_SIZE) {
            throw new IllegalArgumentException("El IV debe ser de 16 bytes.");
        }

        // Aplicar padding PKCS7 al plaintext
        byte[] padded = AESPadding.applyPadding(plaintext);
        byte[] result = new byte[padded.length];

        // Bloque anterior comienza con IV
        byte[] prevBlock = iv.clone();

        for (int i = 0; i < padded.length; i += BLOCK_SIZE) {
            // Extraer bloque actual
            byte[] block = new byte[BLOCK_SIZE];
            System.arraycopy(padded, i, block, 0, BLOCK_SIZE);

            // CBC: XOR con bloque anterior (o IV en el primer bloque)
            byte[] xorBlock = xorBlocks(block, prevBlock);

            // Cifrar bloque con AES
            byte[] encryptedBlock = cipher.encryptBlock(xorBlock);

            // Guardar resultado y actualizar bloque previo
            System.arraycopy(encryptedBlock, 0, result, i, BLOCK_SIZE);
            prevBlock = encryptedBlock;
        }

        return result;
    }

    /**
     * Descifra ciphertext en modo CBC con el IV dado.
     *
     * @param ciphertext bytes cifrados (sin IV prefijado)
     * @param iv         el mismo IV usado en el cifrado
     * @return plaintext original sin padding
     */
    public byte[] decrypt(byte[] ciphertext, byte[] iv) {
        if (iv.length != BLOCK_SIZE) {
            throw new IllegalArgumentException("El IV debe ser de 16 bytes.");
        }
        if (ciphertext.length % BLOCK_SIZE != 0) {
            throw new IllegalArgumentException("El ciphertext debe ser múltiplo de 16 bytes.");
        }

        byte[] result = new byte[ciphertext.length];
        byte[] prevBlock = iv.clone();

        for (int i = 0; i < ciphertext.length; i += BLOCK_SIZE) {
            // Extraer bloque cifrado
            byte[] block = new byte[BLOCK_SIZE];
            System.arraycopy(ciphertext, i, block, 0, BLOCK_SIZE);

            // Descifrar bloque con AES
            byte[] decryptedBlock = decipher.decryptBlock(block);

            // CBC: XOR con bloque anterior (o IV)
            byte[] plainBlock = xorBlocks(decryptedBlock, prevBlock);

            System.arraycopy(plainBlock, 0, result, i, BLOCK_SIZE);
            prevBlock = block; // El bloque cifrado se convierte en el "previo"
        }

        // Remover padding PKCS7
        return AESPadding.removePadding(result);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * XOR byte a byte de dos bloques de 16 bytes.
     */
    private byte[] xorBlocks(byte[] a, byte[] b) {
        byte[] result = new byte[BLOCK_SIZE];
        for (int i = 0; i < BLOCK_SIZE; i++) {
            result[i] = (byte) (a[i] ^ b[i]);
        }
        return result;
    }
}
