package co.edu.uceva.security.aes.tests;

import co.edu.uceva.security.aes.AESBlockCipher;

public class TestAES {

    public static void main(String[] args) {

        String keyHex = "00000000000000000000000000000000";


        //test from https://csrc.nist.gov/Projects/Cryptographic-Algorithm-Validation-Program/Block-Ciphers#AES

        String[][] tests = {
                //          plaintext                       expected ciphertext
                {"f34481ec3cc627bacd5dc3fb08f273e6", "0336763e966d92595a567cc9ce537f5e"},
                {"9798c4640bad75c7c3227db910174e72", "a9a1631bf4996954ebc093957b234589"},
                {"96ab5c2ff612d9dfaae8c31f30c42168", "ff4f8391a6a40ca5b25d23bedd44a597"},
                {"6a118a874519e64e9963798a503f1d35", "dc43be40be0e53712f7e2bf5ca707209"},
                {"cb9fceec81286ca3e989bd979b0cb284", "92beedab1895a94faa69b632e5cc47ce"},
                {"b26aeb1874e47ca8358ff22378f09144", "459264f4798f6a78bacb89c15ed3d601"},
                {"58c8e00b2631686d54eab84b91f0aca1", "08a4e2efec8a8e3312ca7460b9040bbf"}
        };

        byte[] key = hexToBytes(keyHex);
        AESBlockCipher cipher = new AESBlockCipher(key);

        for (int i = 0; i < tests.length; i++) {

            byte[] plaintext = hexToBytes(tests[i][0]);
            String expected = tests[i][1];

            byte[] result = cipher.encryptBlock(plaintext);

            String resultHex = bytesToHex(result);

            System.out.println("COUNT = " + i);
            System.out.println("Expected: " + expected);
            System.out.println("Result:   " + resultHex);

            if (expected.equalsIgnoreCase(resultHex)) {
                System.out.println("✅ PASS");
            } else {
                System.out.println("❌ FAIL");
            }

            System.out.println();
        }
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] result = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            result[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }

        return result;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();

        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }

        return sb.toString();
    }
}