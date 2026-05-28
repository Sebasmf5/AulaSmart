package co.edu.uceva.security.rsa;

import lombok.Getter;

import java.math.BigInteger;

// Esta clase su funcionalidad es almacenar el par de claves RSA (pública y privada) junto con el módulo n.
// Es un contenedor de datos que facilita el manejo de las claves generadas por el algoritmo RSA.

@Getter
public class RSAKeyPair {


    private final BigInteger n; // módulo (p*q)
    private final BigInteger e; // clave pública
    private final BigInteger d; // clave privada

    public RSAKeyPair(BigInteger n, BigInteger e, BigInteger d) {
        this.n = n;
        this.e = e;
        this.d = d;
    }

    public BigInteger getN() { return n; }
    public BigInteger getE() { return e; }
    public BigInteger getD() { return d; }
}
