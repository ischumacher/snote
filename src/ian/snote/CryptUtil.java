package ian.snote;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class CryptUtil {
	static {
		removeCryptographyRestrictions();
		addBouncyCastleProvider();
	}
	final static Charset UTF8 = Charset.forName("utf-8");
	public static void addBouncyCastleProvider() {
		Security.addProvider(new BouncyCastleProvider());
	}
	// 'bytes' must be less than 64
	public static byte[] customConvertToKey(byte pass[], int bytes) {
		if (bytes < 0) {
			throw new IllegalArgumentException("negative 'bytes' not allowed");
		}
		if (bytes > 64) {
			throw new IllegalArgumentException("'bytes' larger than 64 not supported");
		}
		SecureRandom sr;
		try {
			sr = SecureRandom.getInstance("SHA1PRNG");
		} catch (final NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		sr.setSeed(pass);
		final SHA3Digest digest = new SHA3Digest(512);
		final byte pwdb[] = scramble(sr, pass);
		digest.update(pwdb, 0, pwdb.length);
		final byte hash[] = new byte[64];
		digest.doFinal(hash, 0);
		if (bytes < 64) {
			final byte key[] = new byte[bytes];
			System.arraycopy(hash, 0, key, 0, key.length);
			return key;
		}
		return hash;
	}
	// Scramble in a repeatable way
	public static byte[] deterministicScramble(byte arr[]) {
		SecureRandom sr;
		try {
			sr = SecureRandom.getInstance("SHA1PRNG");
		} catch (final NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		sr.setSeed(arr);
		return scramble(sr, arr);
	}
	public static List<String> getAlgorithms() {
		return getAlgorithms("");
	}
	public static List<String> getAlgorithms(String filter) {
		final List<String> list = new ArrayList<>();
		try {
			final Provider p[] = Security.getProviders("BC");
			for (int i = 0; i < p.length; i++) {
				for (final Enumeration<Object> e = p[i].keys(); e.hasMoreElements();) {
					final String name = e.nextElement().toString();
					if (name.contains(filter)) {
						list.add(name);
					}
				}
			}
		} catch (final Exception e) {
			System.out.println(e);
		}
		return list;
	}
	public static boolean isRestrictedCryptography() {
		// This simply matches the Oracle JRE, but not OpenJDK.
		return "Java(TM) SE Runtime Environment".equals(System.getProperty("java.runtime.name"));
	}
	// Scramble in non-determinstic way
	public static byte[] randomScramble(byte arr[]) {
		SecureRandom sr;
		try {
			sr = SecureRandom.getInstanceStrong();
		} catch (final NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		return scramble(sr, arr);
	}
	public static boolean removeCryptographyRestrictions() {
		if (!isRestrictedCryptography()) {
			return true;
		}
		try {
			/*
			 * Do the following, but with reflection to bypass access checks:
			 *
			 * JceSecurity.isRestricted = false;
			 * JceSecurity.defaultPolicy.perms.clear();
			 * JceSecurity.defaultPolicy.add(CryptoAllPermission.INSTANCE);
			 */
			final Class<?> jceSecurity = Class.forName("javax.crypto.JceSecurity");
			final Class<?> cryptoPermissions = Class.forName("javax.crypto.CryptoPermissions");
			final Class<?> cryptoAllPermission = Class.forName("javax.crypto.CryptoAllPermission");
			final Field isRestrictedField = jceSecurity.getDeclaredField("isRestricted");
			isRestrictedField.setAccessible(true);
			final Field modifiersField = Field.class.getDeclaredField("modifiers");
			modifiersField.setAccessible(true);
			modifiersField.setInt(isRestrictedField, isRestrictedField.getModifiers() & ~Modifier.FINAL);
			isRestrictedField.set(null, false);
			final Field defaultPolicyField = jceSecurity.getDeclaredField("defaultPolicy");
			defaultPolicyField.setAccessible(true);
			final PermissionCollection defaultPolicy = (PermissionCollection) defaultPolicyField.get(null);
			final Field perms = cryptoPermissions.getDeclaredField("perms");
			perms.setAccessible(true);
			((Map<?, ?>) perms.get(defaultPolicy)).clear();
			final Field instance = cryptoAllPermission.getDeclaredField("INSTANCE");
			instance.setAccessible(true);
			defaultPolicy.add((Permission) instance.get(null));
			return true;
		} catch (final Exception e) {
			System.out.println("Failed to remove cryptography restrictions " + e.getMessage());
		}
		return false;
	}
	public static byte[] scramble(SecureRandom sr, byte arr[]) {
		final List<Byte> passIn = new ArrayList<>();
		for (int i = 0; i < arr.length; ++i) {
			passIn.add(arr[i]);
		}
		final List<Byte> mixedList = new ArrayList<>();
		while (!passIn.isEmpty()) {
			mixedList.add(passIn.remove(sr.nextInt(passIn.size())));
		}
		final byte mixed[] = new byte[mixedList.size()];
		for (int i = 0; i < mixed.length; ++i) {
			mixed[i] = mixedList.get(i);
		}
		return mixed;
	}
	public static SecretKey standardPasswordToSecretKey(String password, int bitLength) {
		final byte salt[] = new byte[8];
		SecureRandom sr;
		try {
			sr = SecureRandom.getInstance("SHA1PRNG");
		} catch (final NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		sr.setSeed(password.getBytes(UTF8));
		sr.nextBytes(salt);
		SecretKeyFactory factory;
		try {
			factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			final KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, bitLength);
			final SecretKey key = factory.generateSecret(spec);
			return key;
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}
}
