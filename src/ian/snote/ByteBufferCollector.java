package ian.snote;

import java.nio.ByteBuffer;

public class ByteBufferCollector {
	private ByteBuffer buf;
	public ByteBufferCollector() {
		buf = ByteBuffer.allocate(1024);
	}
	public ByteBufferCollector(int size) {
		buf = ByteBuffer.allocate(size);
	}
	public void add(byte b) {
		checkRoom(1);
		buf.put(b);
	}
	public void add(byte b[]) {
		checkRoom(b.length);
		buf.put(b);
	}
	public void add(byte[] b, int off, int length) {
		checkRoom(length);
		buf.put(b, off, length);
	}
	public void add(ByteBuffer buf) {
		checkRoom(buf.remaining());
		buf.put(buf);
	}
	public void add(double x) {
		checkRoom(8);
		buf.putDouble(x);
	}
	public void add(double x[]) {
		checkRoom(8 * x.length);
		for (int i = 0; i < x.length; ++i) {
			buf.putDouble(x[i]);
		}
	}
	public void add(float x) {
		checkRoom(4);
		buf.putFloat(x);
	}
	public void add(float x[]) {
		checkRoom(4 * x.length);
		for (int i = 0; i < x.length; ++i) {
			buf.putFloat(x[i]);
		}
	}
	public void add(int x) {
		checkRoom(4);
		buf.putInt(x);
	}
	public void add(int x[]) {
		checkRoom(4 * x.length);
		for (int i = 0; i < x.length; ++i) {
			buf.putInt(x[i]);
		}
	}
	public void add(long x) {
		checkRoom(8);
		buf.putLong(x);
	}
	public void add(long x[]) {
		checkRoom(8 * x.length);
		for (int i = 0; i < x.length; ++i) {
			buf.putLong(x[i]);
		}
	}
	public void add(short x) {
		checkRoom(2);
		buf.putInt(x);
	}
	public void add(short x[]) {
		checkRoom(2 * x.length);
		for (int i = 0; i < x.length; ++i) {
			buf.putShort(x[i]);
		}
	}
	private void checkRoom(int length) {
		if (length > buf.remaining()) {
			ByteBuffer nbuf;
			if (length > buf.capacity()) {
				nbuf = ByteBuffer.allocate(buf.capacity() + length);
			} else {
				nbuf = ByteBuffer.allocate(buf.capacity() * 2);
			}
			buf.flip();
			nbuf.put(buf);
			buf = nbuf;
		}
	}
	public ByteBuffer getByteBuffer() {
		final ByteBuffer dup = buf.duplicate();
		dup.flip();
		return dup;
	}
}
