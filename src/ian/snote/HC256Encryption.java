package ian.snote;

import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.engines.HC256Engine;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

public class HC256Encryption {
	final static byte seed[] = { 7, -63, -20, 59, -4, 3, -91, -123, -50, -125, -48, 24, 82, -121, 10, -122 };
	public static ByteBuffer decrypt(ByteBuffer encrypted, byte key32bytes[]) {
		final HC256Engine cipher = new HC256Engine();
		CipherParameters params = new ParametersWithIV(new KeyParameter(key32bytes), seed);
		cipher.init(false, params);
		final byte iv[] = new byte[16];
		final byte ivenc[] = new byte[iv.length];
		encrypted.get(ivenc);
		cipher.processBytes(ivenc, 0, ivenc.length, iv, 0);
		params = new ParametersWithIV(new KeyParameter(key32bytes), iv);
		cipher.init(false, params);
		final byte msgenc[] = ByteBufferUtil.toBytes(encrypted);
		final byte msg[] = new byte[msgenc.length];
		cipher.processBytes(msgenc, 0, msgenc.length, msg, 0);
		return ByteBuffer.wrap(msg);
	}
	public static ByteBuffer encrypt(ByteBuffer content, byte key32bytes[]) {
		final ByteBufferCollector buf = new ByteBufferCollector();
		final HC256Engine cipher = new HC256Engine();
		CipherParameters params = new ParametersWithIV(new KeyParameter(key32bytes), seed);
		cipher.init(true, params);
		final byte iv[] = new byte[16];
		SecureRandom sr;
		try {
			sr = SecureRandom.getInstanceStrong();
		} catch (final NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		sr.nextBytes(iv);
		final byte ivenc[] = new byte[iv.length];
		cipher.processBytes(iv, 0, iv.length, ivenc, 0);
		buf.add(ivenc);
		params = new ParametersWithIV(new KeyParameter(key32bytes), iv);
		cipher.init(true, params);
		final byte msg[] = ByteBufferUtil.toBytes(content);
		final byte encrypted[] = new byte[msg.length];
		cipher.processBytes(msg, 0, msg.length, encrypted, 0);
		buf.add(encrypted);
		return buf.getByteBuffer();
	}
}
