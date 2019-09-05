///-------------------------------------------------------------------------------------
///--------------------------------- a package cryptography ----------------------------
///----------------------- to use this package, see the makefile -----------------------
//-------------------------you will need to set classpath for this package -------------
//--------------------------------------------------------------------------------------
package cryptography;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

public class dummy_cryptographyExample {

    private static final String ALGORITHM = "RSA";
    private static final String signature_algo = "SHA256WithRSA";
    public static byte[] encrypt(byte[] publicKey, byte[] inputData)
            throws Exception {
        PublicKey key = KeyFactory.getInstance(ALGORITHM)
                .generatePublic(new X509EncodedKeySpec(publicKey));

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] encryptedBytes = cipher.doFinal(inputData);

        return encryptedBytes;
    }

    public static byte[] decrypt(byte[] privateKey, byte[] inputData)
            throws Exception {
        PrivateKey key = KeyFactory.getInstance(ALGORITHM)
                .generatePrivate(new PKCS8EncodedKeySpec(privateKey));

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);

        byte[] decryptedBytes = cipher.doFinal(inputData);

        return decryptedBytes;
    }

    public static KeyPair generateKeyPair()
            throws NoSuchAlgorithmException, NoSuchProviderException {

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);

        SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");

        // 512 is keysize
        keyGen.initialize(512, random);

        KeyPair generateKeyPair = keyGen.generateKeyPair();
        return generateKeyPair;
    }
    public static byte[] get_signature(PrivateKey ref_private_key, byte[] data)
    { 
	Signature signature;
	SecureRandom secureRandom;
	try
	{
		// instantiating the signature
		signature = Signature.getInstance("NONEWithRSA");
		// initializing the signature
		secureRandom = new SecureRandom();
		signature.initSign(ref_private_key, secureRandom);
		// creating the digital signature
		signature.update(data);	
		return signature.sign();
	}
	catch(Exception e){ System.out.println(e);}
	return null;	
    }
    public static boolean verify_signature(byte[] sig1, PublicKey ref_public_key, byte[] data)
    {
	try
	{
		Signature sig2 = Signature.getInstance("NONEWithRSA");
		sig2.initVerify(ref_public_key);
		sig2.update(data);
		return sig2.verify(sig1);
	}
	catch(Exception e){ System.out.println(e);}
	return false;
    }

	public static void main(String[] args) throws Exception 
	{
		KeyPair generateKeyPair = generateKeyPair();
		PublicKey ref_public_key = generateKeyPair.getPublic() ;
		PrivateKey ref_private_key = generateKeyPair.getPrivate();
		byte[] publicKey = ref_public_key.getEncoded();
		byte[] privateKey = ref_private_key.getEncoded();

		String msg1 = "hello";
		String msg2 = "hello1";
		byte[] sig1 = get_signature(ref_private_key, msg1.getBytes());
		boolean verify = verify_signature(sig1, ref_public_key, msg2.getBytes());
		System.out.println(verify);
//		byte[] encryptedData = encrypt(publicKey,
//			"hi there".getBytes());

//		byte[] decryptedData = decrypt(privateKey, encryptedData);
	}

}

