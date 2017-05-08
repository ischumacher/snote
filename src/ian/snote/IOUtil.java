package ian.snote;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

public class IOUtil {
	public static void copy(ReadableByteChannel src, WritableByteChannel dest) throws IOException {
		final ByteBuffer buffer = ByteBuffer.allocateDirect(8192);
		while (src.read(buffer) != -1) {
			buffer.flip();
			dest.write(buffer);
			buffer.compact();
		}
		buffer.flip();
		while (buffer.hasRemaining()) {
			dest.write(buffer);
		}
	}
	public static ByteBuffer readByteChannel(ReadableByteChannel ch) throws IOException {
		final ByteBufferCollector buf = new ByteBufferCollector();
		final ByteBuffer b = ByteBuffer.allocate(8192);
		while (ch.read(b) > -1) {
			b.flip();
			buf.add(b);
			b.flip();
		}
		ch.close();
		return buf.getByteBuffer();
	}
	public static void readFully(ReadableByteChannel ch, ByteBuffer buf) throws IOException {
		while (buf.hasRemaining()) {
			ch.read(buf);
		}
	}
	public static ByteBuffer readInputStream(InputStream is) throws IOException {
		final ByteBufferCollector buf = new ByteBufferCollector();
		final byte b[] = new byte[8192];
		int read;
		while ((read = is.read(b)) > -1) {
			buf.add(b, 0, read);
		}
		is.close();
		return buf.getByteBuffer();
	}
	public static ByteBuffer readPath(Path p) throws IOException {
		return ByteBuffer.wrap(Files.readAllBytes(p));
	}
	public static String toString(Reader r) throws IOException {
		final StringBuilder sb = new StringBuilder();
		final char ch[] = new char[8192];
		int read;
		while ((read = r.read(ch)) > -1) {
			sb.append(ch, 0, read);
		}
		r.close();
		return sb.toString();
	}
	public static void writeFully(WritableByteChannel ch, ByteBuffer buf) throws IOException {
		while (buf.hasRemaining()) {
			ch.write(buf);
		}
	}
}
