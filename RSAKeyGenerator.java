import java.io.FileOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

public class RSAKeyGenerator {
    public static void main(String[] args) throws Exception {
        // Generate a 2048-bit RSA key pair
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        
        // Extract public and private keys
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();
        
        // Save the public key in the "keys" folder as "server_public.key"
        try (FileOutputStream fos = new FileOutputStream("keys/server_public.key")) {
            fos.write(publicKey.getEncoded());
        }
        // (Optional) Save the private key if you need it for signing
        try (FileOutputStream fos = new FileOutputStream("keys/server_private.key")) {
            fos.write(privateKey.getEncoded());
        }
        System.out.println("RSA 2048-bit server key pair generated and saved.");
    }
}
