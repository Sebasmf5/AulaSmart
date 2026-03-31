package co.edu.uceva.security.rsa.utils;

import java.math.BigInteger;
import java.security.SecureRandom;

// Esta clase se encarga de generar números primos grandes utilizando el test de primalidad de Miller-Rabin
// para asegurar que los números generados sean probablemente primos. Es una parte crucial del proceso de
// generación de claves RSA, ya que la seguridad del algoritmo depende en gran medida de la calidad de los
// números primos utilizados.

public class PrimeGenerator {

    public static BigInteger generatePrime(int bitLength, int certainty) {
        BigInteger prime;
        do {
            prime = randomOddNumber(bitLength);          // generar un candidato impar
        } while (!MillerRabin.isProbablePrime(prime, certainty)); // probar primalidad
        return prime;
    }

    private static BigInteger randomOddNumber(int bitLength) {
        SecureRandom random = new SecureRandom();
        BigInteger number;
        do {
            number = new BigInteger(bitLength, random);
        } while (number.mod(BigInteger.TWO).equals(BigInteger.ZERO)); // asegurar impar
        return number;
    }
}
