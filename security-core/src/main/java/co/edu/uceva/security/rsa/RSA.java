package co.edu.uceva.security.rsa;

import co.edu.uceva.security.rsa.utils.PrimeGenerator;
import java.math.BigInteger;
import java.security.SecureRandom;

public class RSA {

    private static final BigInteger DEFAULT_E = BigInteger.valueOf(65537);
    private static final SecureRandom random = new SecureRandom();

    /**
     * Genera un par de claves RSA de un tamaño en bits específico
     * certainty: número de iteraciones para el test de primalidad (mayor = más seguro de que el número es primo)
     * bitLength: número de bits para cada primo (p y q) que se generará. El módulo n tendrá aproximadamente
     * el doble de este tamaño en bits.
     */
    public static RSAKeyPair generateKeyPair(int bitLength) {
        // 1. Generar primos grandes p y q
        BigInteger p = PrimeGenerator.generatePrime(bitLength, 10).setBit(511);
        BigInteger q = PrimeGenerator.generatePrime(bitLength, 10).setBit(511);

        // 2. Calcular n y phi
        BigInteger n = p.multiply(q);
        BigInteger phi = (p.subtract(BigInteger.ONE)).multiply(q.subtract(BigInteger.ONE));

        // 3. Definir e y calcular d
        BigInteger e = DEFAULT_E;
        if (!gcd(e, phi).equals(BigInteger.ONE)) {
            throw new RuntimeException("e y phi no son coprimos. Genera nuevos primos.");
        }
        BigInteger d = modInverse(e, phi);

        // 4. Retornar contenedor de claves
        return new RSAKeyPair(n, e, d);
    }

    /**
     * Cifrado RSA: cipher = message^e mod n
     */
    public static BigInteger encrypt(BigInteger message, BigInteger e, BigInteger n) {
        return modExp(message, e, n);
    }

    /**
     * Descifrado RSA: message = cipher^d mod n
     */
    public static BigInteger decrypt(BigInteger cipher, BigInteger d, BigInteger n) {
        return modExp(cipher, d, n);
    }

    /**
     * Exponenciación modular rápida
     */
    public static BigInteger modExp(BigInteger base, BigInteger exp, BigInteger mod) {
        BigInteger result = BigInteger.ONE;
        base = base.mod(mod);
        while (exp.compareTo(BigInteger.ZERO) > 0) {
            if (exp.mod(BigInteger.TWO).equals(BigInteger.ONE)) {
                result = result.multiply(base).mod(mod);
            }
            exp = exp.shiftRight(1); // exp / 2
            base = base.multiply(base).mod(mod);
        }
        return result;
    }

    /**
     * Máximo común divisor
     */
    private static BigInteger gcd(BigInteger a, BigInteger b) {
        return b.equals(BigInteger.ZERO) ? a : gcd(b, a.mod(b));
    }

    /**
     * Inverso modular usando algoritmo extendido de Euclides
     */
    public static BigInteger modInverse(BigInteger a, BigInteger m) {
        BigInteger m0 = m;
        BigInteger y = BigInteger.ZERO, x = BigInteger.ONE;

        if (m.equals(BigInteger.ONE)) return BigInteger.ZERO;

        while (a.compareTo(BigInteger.ONE) > 0) {
            BigInteger q = a.divide(m);
            BigInteger t = m;

            m = a.mod(m);
            a = t;
            t = y;

            y = x.subtract(q.multiply(y));
            x = t;
        }

        if (x.compareTo(BigInteger.ZERO) < 0) {
            x = x.add(m0);
        }
        return x;
    }
}