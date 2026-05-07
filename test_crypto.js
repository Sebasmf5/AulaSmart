const crypto = require('crypto');

const RSA_MASTER_N = "4d81da09404cd0ac8fa8ab6ce80d6b0aed35d91be3bfea443ba1a02fe1f4dccf09ce3327d1514270b5402f4fad8cf0e0ba9988f3efa6b8c126958b890af65b764e75b46dcac8ae992815156d38d56f8827a59affa5aaef21c57ae55eca155203c9d53f8efbe0bb41bb601fdd8c5e6bb81313a5c5072b3797ebdcc4e8b13faa03";

const modulusB64url = Buffer.from(RSA_MASTER_N, 'hex').toString('base64url');
const exponentB64url = Buffer.from([0x01, 0x00, 0x01]).toString('base64url');

const jwk = {
    kty: "RSA",
    n: modulusB64url,
    e: exponentB64url
};

const publicKey = crypto.createPublicKey({ key: jwk, format: 'jwk' });

// 1. Generate AES key (16 bytes)
const aesKey = crypto.randomBytes(16);
console.log("Generated AES Key (Hex):", aesKey.toString('hex'));

// 2. Encrypt AES key using RSA (No padding usually in such custom protocols unless PKCS1 is used. Wait, KeyExchangeService uses BigInteger directly!)
// The KeyExchangeService in security-core:
// BigInteger c = new BigInteger(1, cipherBytes);
// BigInteger m = c.modPow(d, n);
// This means raw RSA math, no padding! 
// To do raw RSA math in node:
const cipherInt = BigInt('0x' + aesKey.toString('hex')) ** BigInt(65537) % BigInt('0x' + RSA_MASTER_N);
let encryptedAesKeyHex = cipherInt.toString(16);
if (encryptedAesKeyHex.length % 2 !== 0) encryptedAesKeyHex = '0' + encryptedAesKeyHex;
const encryptedAesKeyBase64 = Buffer.from(encryptedAesKeyHex, 'hex').toString('base64');

console.log("Encrypted AES Key (Base64):", encryptedAesKeyBase64);

async function run() {
    try {
        // 3. Send to key-exchange
        const res1 = await fetch('http://127.0.0.1:8083/api/v1/crypto/key-exchange', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ encryptedAesKey: encryptedAesKeyBase64 })
        });
        const data1 = await res1.json();
        const sessionId = data1.sessionId;
        console.log("Session ID obtained:", sessionId);

        // 4. Encrypt payload
        const payload = JSON.stringify({
            "capacidad": 40,
            "codigoAula": 101,
            "codigoDependencia": "DEP-01",
            "codigoEdificio": "EDF-01",
            "codigoTipoAula": "TIPO-01",
            "nombreAula": "Laboratorio XYZ",
            "nombreDependencia": "Ingenieria",
            "nombreEdificio": "Facultad de Ingenieria",
            "nombreTipoAula": "Informatica",
            "version": 0
        });

        const cipher = crypto.createCipheriv('aes-128-ecb', aesKey, null);
        let encrypted = cipher.update(payload, 'utf8', 'base64');
        encrypted += cipher.final('base64');

        console.log("Encrypted Payload (Base64):", encrypted);

        // 5. Send POST request
        const res2 = await fetch('http://127.0.0.1:8083/api/v1/aula-service/aulas', {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json',
                'X-Session-ID': sessionId
            },
            body: JSON.stringify({ encryptedData: encrypted, sessionId: sessionId })
        });
        
        const data2 = await res2.json();
        console.log("Response JSON:", data2);
        
        if (data2.encryptedData) {
            // Decrypt response
            const decipher = crypto.createDecipheriv('aes-128-ecb', aesKey, null);
            let decrypted = decipher.update(data2.encryptedData, 'base64', 'utf8');
            decrypted += decipher.final('utf8');
            console.log("Decrypted Response Payload:", decrypted);
        }

    } catch (e) {
        console.error("Error:", e);
    }
}

run();
