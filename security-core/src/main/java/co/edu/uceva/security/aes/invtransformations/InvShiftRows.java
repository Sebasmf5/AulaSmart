package co.edu.uceva.security.aes.invtransformations;

public class InvShiftRows {
    public static void invShiftRows(byte[][] state) {
        // Shift the second row to the right by 1
        byte temp = state[1][3];
        state[1][3] = state[1][2];
        state[1][2] = state[1][1];
        state[1][1] = state[1][0];
        state[1][0] = temp;

        // Shift the third row to the right by 2
        temp = state[2][0];
        byte temp2 = state[2][1];
        state[2][0] = state[2][2];
        state[2][1] = state[2][3];
        state[2][2] = temp;
        state[2][3] = temp2;

        // Shift the fourth row to the right by 3 (or left by 1)
        temp = state[3][0];
        state[3][0] = state[3][1];
        state[3][1] = state[3][2];
        state[3][2] = state[3][3];
        state[3][3] = temp;
    }
}
