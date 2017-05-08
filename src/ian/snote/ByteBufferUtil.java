package ian.snote;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.List;

public class ByteBufferUtil {
	private static final Charset UTF8 = Charset.forName("utf-8");
	public static ByteBuffer combine(ByteBuffer... list) {
		int size = 0;
		for (final ByteBuffer x : list) {
			size += x.remaining();
		}
		final ByteBuffer buf = ByteBuffer.allocate(size);
		for (final ByteBuffer x : list) {
			buf.put(x);
		}
		buf.flip();
		return buf;
	}
	public static ByteBuffer combine(List<ByteBuffer> list) {
		int size = 0;
		for (final ByteBuffer x : list) {
			size += x.remaining();
		}
		final ByteBuffer buf = ByteBuffer.allocate(size);
		for (final ByteBuffer x : list) {
			buf.put(x);
		}
		buf.flip();
		return buf;
	}
	public static ByteBuffer fromBase64(String base64) {
		return ByteBuffer.wrap(Base64.getDecoder().decode(base64));
	}
	public static String toBase64(ByteBuffer buf) {
		return Base64.getEncoder().encodeToString(toBytes(buf));
	}
	public static ByteBuffer toByteBuffer(int value) {
		final ByteBuffer buf = ByteBuffer.allocate(4);
		buf.order(ByteOrder.BIG_ENDIAN);
		buf.putInt(value);
		return buf;
	}
	public static ByteBuffer toByteBuffer(long value) {
		final ByteBuffer buf = ByteBuffer.allocate(8);
		buf.order(ByteOrder.BIG_ENDIAN);
		buf.putLong(value);
		buf.flip();
		return buf;
	}
	public static byte[] toBytes(ByteBuffer msg) {
		final byte arr[] = new byte[msg.remaining()];
		msg.get(arr);
		return arr;
	}
	public static byte[] toBytes(ByteBuffer msg, int length) {
		final byte arr[] = new byte[length];
		msg.get(arr);
		return arr;
	}
	public static String toString(ByteBuffer buf) {
		return new String(toBytes(buf), UTF8);
	}
}
