package co.edu.uceva.security.rsa;


import java.math.BigInteger;

public class RSA {

    public static BigInteger modExp(BigInteger base, BigInteger exp, BigInteger mod) {
        BigInteger result = BigInteger.ONE;
        base = base.mod(mod);
        while (exp.compareTo(BigInteger.ZERO) > 0) {
            if (exp.and(BigInteger.ONE).equals(BigInteger.ONE)) {
                result = (result.multiply(base)).mod(mod);
            }
            exp = exp.shiftRight(1); // exp = exp / 2 (shift right es equivalente a dividir entre 2)
            base = (base.multiply(base)).mod(mod); // sirve para reducir el numero de multiplicaciones necesarias
        }
        return result;
    }

}
