package co.edu.uceva.security.rsa;

import java.math.BigInteger;

public class TestRSA {
    public static void main(String[] args) {

        // 1️⃣ Generar claves RSA
        RSAKeyPair keyPair = RSA.generateKeyPair(512);

        String mensajeTexto = "Hola, este es un mensaje secreto que vamos a cifrar usando RSA con padding!";

        System.out.println("Mensaje original: " + mensajeTexto);
        System.out.println("--------------------------------------");

        for (int i = 1; i <= 3; i++) {

            byte[] mensajeBytes = mensajeTexto.getBytes();

            // 2️⃣ Tamaño del bloque RSA
            int blockSize = (keyPair.getN().bitLength() + 7) / 8;

            // 3️⃣ Aplicar padding (aleatorio)
            byte[] padded = RSAPadding.addPadding(mensajeBytes, blockSize);

            // 4️⃣ Convertir a número
            BigInteger mensaje = new BigInteger(1, padded);

            // 5️⃣ Cifrar
            BigInteger cifrado = RSA.encrypt(mensaje, keyPair.getE(), keyPair.getN());

            // 6️⃣ Descifrar
            BigInteger descifrado = RSA.decrypt(cifrado, keyPair.getD(), keyPair.getN());

            // 7️⃣ Convertir a bytes
            byte[] descifradoBytes = descifrado.toByteArray();

            if (descifradoBytes.length < blockSize) {
                byte[] tmp = new byte[blockSize];
                System.arraycopy(descifradoBytes, 0, tmp, blockSize - descifradoBytes.length, descifradoBytes.length);
                descifradoBytes = tmp;
            }

            // 8️⃣ Quitar padding
            byte[] mensajeOriginal = RSAPadding.removePadding(descifradoBytes);

            String textoDescifrado = new String(mensajeOriginal);

            System.out.println("Intento #" + i);
            System.out.println("Cifrado: " + cifrado);
            System.out.println("Texto descifrado: " + textoDescifrado);
            System.out.println("--------------------------------------");
            System.out.println("N,E,D:\n " + keyPair.getN() + "\n " + keyPair.getE() + "\n " + keyPair.getD()+"\n");
            System.out.println("bits totales: " + keyPair.getN().bitLength());
        }
    }
}