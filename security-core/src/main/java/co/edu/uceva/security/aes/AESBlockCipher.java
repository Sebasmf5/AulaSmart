package co.edu.uceva.security.aes;
//Aqui es donde se implementa el bloque de cifrado AES, utilizando las transformaciones
// y la expansión de claves definidas en los paquetes correspondientes.
import co.edu.uceva.security.aes.transformations.*;

public class AESBlockCipher {
    private static final int NB = 4; // Number of columns (32-bit words) comprising the State. For AES, NB = 4.
    private static final int NR = 10; // Number of rounds, which is a function of the key size. For AES-128, NR = 10.

    private final byte [][][] roundKeys; // Round keys for each round
    public AESBlockCipher(byte[] key) {
        if (key.length != 16) { // AES-128 requires a 16-byte key
            throw new IllegalArgumentException("Key must be 16 bytes long for AES-128.");
        }
        this.roundKeys = AESKeyExpansion.expandKey(key);
    }

    public byte[] encryptBlock(byte[] input) {
        if (input.length != 16) { // AES operates on 16-byte blocks
            throw new IllegalArgumentException("Input block must be 16 bytes long.");
        }

        // Convert input to state matrix
        byte[][] state = new byte[4][4];
        for (int i = 0; i < input.length; i++) {
            state[i % 4][i / 4] = input[i];
        }

        // Initial round key addition
        AddRoundKey.addRoundKey(state, roundKeys[0]);

        // Main rounds
        for (int round = 1; round < NR; round++) {
            SubBytes.subBytes(state);
            ShiftRows.shiftRows(state);
            MixColumns.mixColumns(state);
            AddRoundKey.addRoundKey(state, roundKeys[round]);
        }

        // Final round (no MixColumns)
        SubBytes.subBytes(state);
        ShiftRows.shiftRows(state);
        AddRoundKey.addRoundKey(state, roundKeys[NR]);

        // Convert state matrix back to output byte array
        byte[] output = new byte[16];
        for (int i = 0; i < output.length; i++) {
            output[i] = state[i % 4][i / 4];
        }
        return output;
    }
}
