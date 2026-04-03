package co.edu.uceva.security.aes.transformations;

public class ShiftRows {

    public static void shiftRows(byte[][] state) {
        // Shift the second row to the left by 1
        byte temp = state[1][0];
        state[1][0] = state[1][1];
        state[1][1] = state[1][2];
        state[1][2] = state[1][3];
        state[1][3] = temp;

        // Shift the third row to the left by 2
        temp = state[2][0];
        byte temp2 = state[2][1];
        state[2][0] = state[2][2];
        state[2][1] = state[2][3];
        state[2][2] = temp;
        state[2][3] = temp2;

        // Shift the fourth row to the left by 3 (or right by 1)
        temp = state[3][0];
        state[3][0] = state[3][3];
        state[3][3] = state[3][2];
        state[3][2] = state[3][1];
        state[3][1] = temp;
    }

    //test

    public static void main(String[] args) {
        byte[][] state = {
            {0x00, 0x01, 0x02, 0x03},
            {0x10, 0x11, 0x12, 0x13},
            {0x20, 0x21, 0x22, 0x23},
            {0x30, 0x31, 0x32, 0x33}
        };

        System.out.println("Before ShiftRows:");
        printState(state);

        shiftRows(state);

        System.out.println("After ShiftRows:");
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
