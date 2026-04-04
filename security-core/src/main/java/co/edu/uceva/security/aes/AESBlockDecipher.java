package co.edu.uceva.security.aes;

import co.edu.uceva.security.aes.invtransformations.InvSubBytes;
import co.edu.uceva.security.aes.invtransformations.InvShiftRows;
import co.edu.uceva.security.aes.invtransformations.InvMixColumns;

public class AESBlockDecipher {
    private static final int NB = 4; // Number of columns in State
    private static final int NR = 10; // Number of rounds for AES-128

    private final byte[][][] roundKeys; // Round keys for each round

    public AESBlockDecipher(byte[] key) {
        if (key.length != 16) { // AES-128 requires a 16-byte key
            throw new IllegalArgumentException("Key must be 16 bytes long for AES-128.");
        }
        this.roundKeys = AESKeyExpansion.expandKey(key);
    }

    public byte[] decryptBlock(byte[] input) {
        if (input.length != 16) { // AES operates on 16-byte blocks
            throw new IllegalArgumentException("Input block must be 16 bytes long.");
        }

        // Convert input to state matrix
        byte[][] state = new byte[4][4];
        for (int i = 0; i < input.length; i++) {
            state[i % 4][i / 4] = input[i];
        }

        // Initial round: AddRoundKey with last round key
        AddRoundKey.addRoundKey(state, roundKeys[NR]);

        // Main rounds (NR-1 down to 1)
        for (int round = NR - 1; round >= 1; round--) {
            InvShiftRows.invShiftRows(state);
            InvSubBytes.invSubBytes(state);
            AddRoundKey.addRoundKey(state, roundKeys[round]);
            InvMixColumns.invMixColumns(state);
        }

        // Final round (round 0, no InvMixColumns)
        InvShiftRows.invShiftRows(state);
        InvSubBytes.invSubBytes(state);
        AddRoundKey.addRoundKey(state, roundKeys[0]);

        // Convert state matrix back to output byte array
        byte[] output = new byte[16];
        for (int i = 0; i < output.length; i++) {
            output[i] = state[i % 4][i / 4];
        }
        return output;
    }
}