package co.edu.uceva.security.aes.tests;

import co.edu.uceva.security.aes.AESKeyExpansion;

public class TestAESKeyExpansion {

    public static void main(String[] args) {
        // Clave de ejemplo (16 bytes)
        byte[] key = new byte[] {
                (byte) 0x2b, (byte) 0x28, (byte) 0xab, (byte) 0x09,
                (byte) 0x7e, (byte) 0xae, (byte) 0xf7, (byte) 0xcf,
                (byte) 0x15, (byte) 0xd2, (byte) 0x15, (byte) 0x4f,
                (byte) 0x16, (byte) 0xa6, (byte) 0x88, (byte) 0x3c
        };

        // Expandir la clave
        // rounds, rows, columns
        byte[][][] roundKeys = AESKeyExpansion.expandKey(key);

        // Imprimir todas las subclaves
        for (int round = 0; round < roundKeys.length; round++) {
            System.out.println("Round " + round + " Key:");
            printMatrix(roundKeys[round]);
            System.out.println();
        }
    }

    // Función auxiliar para imprimir una matriz 4x4 de bytes en hexadecimal
    private static void printMatrix(byte[][] matrix) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                System.out.printf("%02X ", matrix[i][j]);
            }
            System.out.println();
        }
    }
}