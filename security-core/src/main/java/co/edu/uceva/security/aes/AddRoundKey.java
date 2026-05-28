package co.edu.uceva.security.aes;

public class AddRoundKey {

    public static void addRoundKey(byte[][] state, byte[][] roundKey) {
        if(state.length != 4 || state[0].length != 4) {
            throw new IllegalArgumentException("State must be a 4x4 byte array.");
        }
        for (int row = 0; row < state.length; row++) {
            if (state[row].length != 4) {
                throw new IllegalArgumentException("State must be a 4x4 byte array.");
            }
            for (int col = 0; col < state[row].length; col++) {
                state[row][col] ^= roundKey[row][col];
            }
        }
    }
}
