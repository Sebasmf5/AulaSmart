package co.edu.uceva.security.aes.transformations;

public class MixColumns {

    // La cosa esa que es la que mezcla con el xor y esas cosas
    public static final byte [][] MIX_COLUMNS_MATRIX = {
            {(byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x01},
            {(byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x01},
            {(byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03},
            {(byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x02}
    };

    public static void mixColumns(byte[][] state) {

        byte[][] temp = new byte[4][4];

        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                temp[row][col] = (byte) (
                        gmul(MIX_COLUMNS_MATRIX[row][0], state[0][col]) ^
                        gmul(MIX_COLUMNS_MATRIX[row][1], state[1][col]) ^
                        gmul(MIX_COLUMNS_MATRIX[row][2], state[2][col]) ^
                        gmul(MIX_COLUMNS_MATRIX[row][3], state[3][col])
                );
            }
        }
        // Copy the temporary state back to the original state
        for (int row = 0; row < 4; row++) {
            System.arraycopy(temp[row], 0, state[row], 0, 4);
        }
    }

    // Multiplicación en GF(2^8) usando el metodo de "Russian Peasant Multiplication"
    private static byte gmul(byte a, byte b) {
        byte result = 0;
        byte temp = a;

        for (int i = 0; i < 8; i++) {
            if ((b & 1) == 1) {
                result ^= temp;
            }
            boolean highBitSet = (temp & 0x80) != 0;
            temp <<= 1;
            //condicion por si el bit es el mas alto, se hace el xor para no desbordar el byte
            if (highBitSet) {
                temp ^= 0x1b; // Irreducible polynomial
            }
            b >>= 1;
        }

        return result;
    }

    //test
    public static void main(String[] args) {
        byte[][] state = {
            {0x01, 0x02, 0x03, 0x04},
            {0x05, 0x06, 0x07, 0x08},
            {0x09, 0x0a, 0x0b, 0x0c},
            {0x0d, 0x0e, 0x0f, 0x10}
        };

        System.out.println("Before MixColumns:");
        printState(state);

        mixColumns(state);

        System.out.println("After MixColumns:");
        printState(state);
    }

    private static void printState(byte[][] state) {
        for (byte[] row : state) {
            for (byte b : row) {
                System.out.printf("%02x ", b);
            }
            System.out.println();
        }
    }
}
