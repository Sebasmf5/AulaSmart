package co.edu.uceva.security.rsa;

import co.edu.uceva.security.rsa.utils.RSAUtils;

import java.math.BigInteger;
public class TestRSA {
    public static void main(String[] args) {
        // 1. Generar par de claves de 512 bits
        RSAKeyPair keyPair = RSA.generateKeyPair(512);
        // 2. Mensaje a cifrar
        BigInteger mensaje = RSAUtils.stringToBigInteger("Hola Mundo");
        // 3. Cifrar
        BigInteger cifrado = RSA.encrypt(mensaje, keyPair.getE(), keyPair.getN());

        // 4. Descifrar
        BigInteger descifrado = RSA.decrypt(cifrado, keyPair.getD(), keyPair.getN());

        // 4. Convertir número otra vez a texto
        String textoDescifrado = RSAUtils.bigIntegerToString(descifrado);

        System.out.println("Mensaje numérico: " + mensaje);
        System.out.println("Cifrado: " + cifrado);
        System.out.println("Descifrado numérico: " + descifrado);
        System.out.println("Texto descifrado: " + textoDescifrado);

        System.out.println("CLAVE PUBLICA:");
        System.out.println("e: " + keyPair.getE());
        System.out.println("n: " + keyPair.getN());

        System.out.println("\nCLAVE PRIVADA:");
        System.out.println("d: " + keyPair.getD());
        System.out.println("n: " + keyPair.getN());
        System.out.println("cantidad bits:" + keyPair.getN().bitLength());
    }
}