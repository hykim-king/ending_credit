package com.endit.cmn;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * <pre>
 * Class Name : AESCryptoUtil Description : 양방향 암호화
 *
 * @author user
 * @since 2026. 8. 3.
 */
@Component
public class AESCryptoUtil {
	final Logger log = LoggerFactory.getLogger(getClass());

	public static final String ALGORITHM = "AES";

	final static String ENV_KEY = "SECRET_KEY";

	public AESCryptoUtil() {
		super();

		log.debug("==============================");
		log.debug("{}.{}()", getClass().getSimpleName(), "AESCryptoUtil");
		log.debug("==============================");
	}

	public SecretKeySpec getKey() {
		String key = System.getenv(ENV_KEY);

		return new SecretKeySpec(key.getBytes(), ALGORITHM);
	}

	/**
	 * 복호화
	 *
	 * @param encryptedString
	 * @return String(plainText)
	 */
	public String decrypt(String encryptedString) throws Exception {
		String decryptedString = "";

		// 바이트 배열로 부터 AES 키 객체 생성
		SecretKeySpec key = getKey();

		// AES 알고리즘 암호화 객체 생성
		Cipher cipher = Cipher.getInstance(ALGORITHM);

		// key 복호화 모드로 초기화
		cipher.init(Cipher.DECRYPT_MODE, key);

		// Base64로 디코딩
		byte[] decoded = Base64.getDecoder().decode(encryptedString);
		byte[] decrypted = cipher.doFinal(decoded);

		// UTF-8 처리
		decryptedString = new String(decrypted, "UTF-8");

		return decryptedString;
	}

	/**
	 * 암호화
	 *
	 * @param plainText
	 * @return String(encryptText)
	 * @throws NoSuchPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 * @throws UnsupportedEncodingException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 */
	public String encrypt(String plainText) throws Exception {
		String encryptedBase64String = "";

		// 바이트 배열로 부터 AES 키 객체 생성
		SecretKeySpec key = getKey();

		// AES 알고리즘 암호화 객체 생성
		Cipher cipher = Cipher.getInstance(ALGORITHM);

		// key 암호화 모드로 초기화
		cipher.init(Cipher.ENCRYPT_MODE, key);

		// 입력 문자열을 UTF-8로 바이트 변환 -> AES암호화
		byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));

		// encrypted 결과는 바이너리 -> Base64로 인코딩
		encryptedBase64String = Base64.getEncoder().encodeToString(encrypted);

		return encryptedBase64String;
	}
}
