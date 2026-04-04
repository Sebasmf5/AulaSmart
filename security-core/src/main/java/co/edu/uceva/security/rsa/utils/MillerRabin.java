package co.edu.uceva.security.rsa.utils;

import co.edu.uceva.security.rsa.RSA;

import java.math.BigInteger;
import java.security.SecureRandom;

public class MillerRabin {
/*
 Algoritmo de prueba de primalidad Miller-Rabin
 Este algoritmo determina si un número n es probablemente primo.
 Parámetros:
 - n : número que queremos verificar si es primo
 - k : número de iteraciones del test (mayor k = mayor precisión)
 Pasos del algoritmo:
 1. Casos base
    - Si n <= 1 → no es primo.
    - Si n == 2 o n == 3 → es primo.
    - Si n es par → no es primo.
 2. Descomponer n - 1 en la forma:
        n - 1 = 2^r * d
    donde:
    - d es un número impar
    - r es la cantidad de veces que podemos dividir entre 2.
 3. Repetir el test k veces (iteraciones):
    3.1 Elegir un número aleatorio a en el rango:
        2 ≤ a ≤ n - 2
    3.2 Calcular:
        x = a^d mod n
    3.3 Si:
        x == 1  o  x == n - 1
        entonces esta iteración pasa y continuamos con la siguiente.
    3.4 Si no, repetir r - 1 veces:
        x = x^2 mod n
        Si x == n - 1
           → la iteración pasa (continuar con la siguiente).
        Si x == 1
           → n NO es primo (terminar y retornar false).
 4. Si después de las r iteraciones x nunca fue n - 1
    → n NO es primo.
 5. Si todas las k iteraciones pasan
    → n es "probablemente primo".
 Nota:
 Miller-Rabin es un algoritmo probabilístico.
 Mientras mayor sea k, menor será la probabilidad de error.
*/

    public static boolean isProbablePrime(BigInteger  n, int k) {
        if (n.compareTo(BigInteger.ONE) <= 0) return false;               // n <= 1
        if (n.compareTo(BigInteger.valueOf(3)) <= 0) return true;         // n <= 3
        if (n.mod(BigInteger.TWO).equals(BigInteger.ZERO)) return false;  // n % 2 == 0

        BigInteger d = n.subtract(BigInteger.ONE);
        int r = 0;
        while (d.mod(BigInteger.valueOf(2)).equals(BigInteger.ZERO)) {
            d = d.divide(BigInteger.valueOf(2));  // d = d / 2, d debe ser impar
            r++;                                  // r es el número de veces que dividimos entre 2
        }

        SecureRandom random = new SecureRandom();
        for (int i = 0; i < k; i++) {
            BigInteger nMinus4 = n.subtract(BigInteger.valueOf(4)); // para asegurar que a esté en el rango [2, n-2], ya que n-2 >= 2 para n > 4
            BigInteger a; // número aleatorio a en el rango [2, n-2]
            do {
                a = new BigInteger(nMinus4.bitLength(), random);
            } while (a.compareTo(BigInteger.ZERO) < 0 || a.compareTo(nMinus4) > 0);// aseguramos que a esté en el rango [0, n-4]
            a = a.add(BigInteger.TWO); // ajustamos a para que esté en el rango [2, n-2]
            BigInteger x = RSA.modExp(a, d, n); // x = a^d mod n
            if (x.equals(BigInteger.ONE) || x.equals(n.subtract(BigInteger.ONE))) continue; // si x es 1 o n-1, esta iteración pasa y continuamos con la siguiente

            boolean composite = true;  // asumimos que n es compuesto hasta que se demuestre lo contrario
            for (int j = 0; j < r - 1; j++) {
                x = RSA.modExp(x, BigInteger.TWO, n); // x = x^2 mod n
                if (x.equals(n.subtract(BigInteger.ONE))) { // si x es n-1, esta iteración pasa y continuamos con la siguiente
                    composite = false; // n podría ser primo, así que no marcamos como compuesto y
                    break;
                }
                if (x.equals(BigInteger.ONE)) return false; // si x es 1, n no es primo, terminamos y retornamos false
            }
            if (composite) return false; // si después de las r iteraciones x nunca fue n-1, n no es primo
        }
        return true; // si todas las k iteraciones pasan, n es "probablemente primo"
    }
}
