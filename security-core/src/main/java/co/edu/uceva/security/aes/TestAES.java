package co.edu.uceva.security.aes;

public class TestAES {

    public static void main(String[] args) {

        // Clave del test NIST
        byte[] key = {
                (byte)0x2b,(byte)0x7e,(byte)0x15,(byte)0x16,
                (byte)0x28,(byte)0xae,(byte)0xd2,(byte)0xa6,
                (byte)0xab,(byte)0xf7,(byte)0x15,(byte)0x88,
                (byte)0x09,(byte)0xcf,(byte)0x4f,(byte)0x3c
        };

        // Texto plano
        byte[] plaintext = {
                (byte)0x32,(byte)0x43,(byte)0xf6,(byte)0xa8,
                (byte)0x88,(byte)0x5a,(byte)0x30,(byte)0x8d,
                (byte)0x31,(byte)0x31,(byte)0x98,(byte)0xa2,
                (byte)0xe0,(byte)0x37,(byte)0x07,(byte)0x34
        };

        AESBlockCipher aes = new AESBlockCipher(key);

        byte[] ciphertext = aes.encryptBlock(plaintext);

        System.out.println("Ciphertext obtenido:");
        printHex(ciphertext);

        System.out.println("\nCiphertext esperado:");
        System.out.println("3925841d02dc09fbdc118597196a0b32");
    }

    private static void printHex(byte[] data) {
        for (byte b : data) {
            System.out.printf("%02x", b);
        }
        System.out.println();
    }
}