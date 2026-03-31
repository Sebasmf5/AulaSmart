package co.edu.uceva.security.rsa.utils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

public class RSAUtils {

    // Convierte una cadena de texto a un BigInteger utilizando UTF-8
    public static BigInteger stringToBigInteger(String text) {
        return new BigInteger(1, text.getBytes(StandardCharsets.UTF_8));
    }

    // Convierte un BigInteger a una cadena de texto utilizando UTF-8
    public static String bigIntegerToString(BigInteger number) {
        return new String(number.toByteArray(), StandardCharsets.UTF_8);
    }

}