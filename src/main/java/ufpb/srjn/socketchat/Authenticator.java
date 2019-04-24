package ufpb.srjn.socketchat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.NoSuchPaddingException;

/**
 *
 * @author samueldemoura
 */
public class Authenticator {

	// Logger handle
	private static final Logger LOGGER = Logger.getLogger(ClientThread.class.getName());
	
	// Crytography related fields
	private static String password;
	private static String salt = "PBKDF2WithHmacSHA256";
	
	private static IvParameterSpec iv_spec;
	private static SecretKeyFactory sk_factory;
	private static KeySpec key_spec;
	private static SecretKey tmp_key;
	private static SecretKeySpec secret_key_spec;
	private static Cipher cipher;
	
	/**
	 * Set a new server password.
	 * @param password Server password. 
	 */
	public static void setPassword(String password) {
		// Set the password
		Authenticator.password = password;
		
		// Then, initialize everything else based on that password
		byte[] iv = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		iv_spec = new IvParameterSpec(iv);

		try {
			sk_factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			key_spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 65536, 128);
			tmp_key = sk_factory.generateSecret(key_spec);
			secret_key_spec = new SecretKeySpec(tmp_key.getEncoded(), "AES");
			cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
		} catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException ex) {
			throw new RuntimeException("Error during encryption algorithm setup: " + ex.getMessage());
		}
	}
	
	/**
	 * Returns hashed version of the server password.
	 * @return Hashed password. 
	 */
	public static String getHashedPassword() {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] encoded_hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
			return bytesToHex(encoded_hash);
		} catch (NoSuchAlgorithmException ex) {
			throw new RuntimeException("SHA-256 is unsupported: " + ex.getMessage());
		}
	}
	
	/**
	 * Helper function: creates a hexadecimal representation for a given byte array.
	 * @param hash Original byte array.
	 * @return Hex representation of the byte array.
	 */
	private static String bytesToHex(byte[] hash) {
		StringBuilder hexString = new StringBuilder();
		
		for (int i = 0; i < hash.length; i++) {
			String hex = Integer.toHexString(0xff & hash[i]);
			
			if(hex.length() == 1) hexString.append('0');
			hexString.append(hex);
		}
		
		return hexString.toString();
	}
	
	/**
	 * Encrypts a string.
	 * @param str
	 * @return Encrypted string.
	 */
	public static String encrypt(String str) {
		try {
			cipher.init(Cipher.ENCRYPT_MODE, secret_key_spec, iv_spec);
			return Base64.getEncoder().encodeToString(cipher.doFinal(str.getBytes("UTF-8")));
		} catch (Exception ex) {
			System.out.println("Error while encrypting: " + ex.toString());
			return null;
		}
	}
	
	/**
	 * Decrypts a string.
	 * @param str
	 * @return Decrypted string. 
	 */
	public static String decrypt(String str) {
		try {
			cipher.init(Cipher.DECRYPT_MODE, secret_key_spec, iv_spec);
			return new String(cipher.doFinal(Base64.getDecoder().decode(str)));
		} catch (Exception ex) {
			System.out.println("Error while decrypting: " + ex.toString());
			return null;
		}
	}
}
