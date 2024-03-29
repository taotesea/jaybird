/*
 * Firebird Open Source J2ee connector - jdbc driver
 *
 * Distributable under LGPL license.
 * You may obtain a copy of the License at http://www.gnu.org/copyleft/lgpl.html
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * LGPL License for more details.
 *
 * This file was created by members of the firebird development team.
 * All individual contributions remain the Copyright (C) of those
 * individuals.  Contributors to this file are either listed here or
 * can be obtained from a CVS history command.
 *
 * All rights reserved.
 */
/*
 * The Original Code is the Firebird Java GDS implementation.
 *
 * The Initial Developer of the Original Code is Alejandro Alberola.
 * Portions created by Alejandro Alberola are Copyright (C) 2001
 * Boix i Oltra, S.L. All Rights Reserved.
 *
 */
package org.firebirdsql.gds.impl.wire;

import org.firebirdsql.encodings.Encoding;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * <code>XdrInputStream</code> is an input stream for reading in data that
 * is in the XDR format. An <code>XdrInputStream</code> instance is wrapped
 * around an underlying <code>java.io.InputStream</code>.
 * <p>
 * This class is not thread-safe.
 * </p>
 *
 * @author <a href="mailto:alberola@users.sourceforge.net">Alejandro Alberola</a>
 * @author <a href="mailto:d_jencks@users.sourceforge.net">David Jencks</a>
 * @author <a href="mailto:mrotteveel@users.sourceforge.net">Mark Rotteveel</a>
 * @version 1.0
 */
public final class XdrInputStream extends BufferedInputStream {

    private static final int DEFAULT_BUFFER_SIZE = 16384;

    /**
     * Create a new instance of <code>XdrInputStream</code>.
     *
     * @param in The underlying <code>InputStream</code> to read from
     */
    public XdrInputStream(InputStream in) {
        super(in, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Skips the padding after a buffer of the specified length. The number of bytes to skip is calculated as
     * <code>(4 - length) & 3</code>.
     *
     * @param length
     *         Length of the previously read buffer
     * @return Actual number of bytes skipped
     * @throws IOException
     *         IOException if an error occurs while reading from the
     *         underlying input stream
     * @see XdrOutputStream#writePadding(int, int)
     */
    public int skipPadding(int length) throws IOException {
        int bytesToSkip = (4 - length) & 3;
        int actual = skipFully(bytesToSkip);
        assert actual == bytesToSkip
                : String.format("Unexpected number of bytes skipped: %d, expected: %d", actual, bytesToSkip);
        return actual;
    }

    /**
     * Skips the specified number of bytes.
     *
     * @param numbytes
     *         Number of bytes to skip.
     * @return Actual number of bytes skipped (usually {@code numbytes}, unless the underlying input stream is closed).
     * @throws IOException
     *         IOException if an error occurs while reading from the underlying input stream
     */
    public int skipFully(int numbytes) throws IOException {
        // This is the skip from SocketInputStream, which will read all bytes unless the stream is closed.
        // We can't rely on InputStream.skip(int), because for example CipherInputStream will simply not skip beyond
        // its current buffer.
        if (numbytes <= 0) {
            return 0;
        }
        // TODO Switch the readNBytes in Java 9, and simplify as it's currently only used for 1 - 3 bytes.
        int n = numbytes;
        int buflen = Math.min(1024, n);
        byte[] data = new byte[buflen];
        while (n > 0) {
            int r = read(data, 0, Math.min(buflen, n));
            if (r < 0) {
                break;
            }
            n -= r;
        }
        return numbytes - n;
    }

    /**
     * Read in a byte buffer.
     *
     * @return The buffer that was read
     * @throws IOException if an error occurs while reading from the
     *         underlying input stream
     */
    public byte[] readBuffer() throws IOException {
        int len = readInt();
        byte[] buffer = new byte[len];
        readFully(buffer, 0, len);
        skipPadding(len);
        return buffer;
    }

    /**
     * Read in a raw array of bytes.
     *
     * @param len The number of bytes to read
     * @return The byte buffer that was read
     * @throws IOException if an error occurs while reading from the
     *         underlying input stream
     */
    public byte[] readRawBuffer(int len) throws IOException {
        byte[] buffer = new byte[len];
        readFully(buffer, 0, len);
        return buffer;
    }

    /**
     * Read in a <code>String</code>.
     *
     * @return The <code>String</code> that was read
     * @throws IOException if an error occurs while reading from the
     *         underlying input stream
     */
    public String readString(Encoding encoding) throws IOException {
        byte[] buffer = readBuffer();
        return encoding.decodeFromCharset(buffer);
    }

    private final byte[] readBuffer = new byte[8];

    /**
     * Read in a <code>long</code>.
     *
     * @return The <code>long</code> that was read
     * @throws IOException if an error occurs while reading from the
     *         underlying input stream
     */
    public long readLong() throws IOException {
        readFully(readBuffer, 0, 8);
        return (((long) readBuffer[0] << 56) +
                ((long) (readBuffer[1] & 0xFF) << 48) +
                ((long) (readBuffer[2] & 0xFF) << 40) +
                ((long) (readBuffer[3] & 0xFF) << 32) +
                ((long) (readBuffer[4] & 0xFF) << 24) +
                ((readBuffer[5] & 0xFF) << 16) +
                ((readBuffer[6] & 0xFF) << 8) +
                ((readBuffer[7] & 0xFF)));
    }

    /**
     * Read in an <code>int</code>.
     *
     * @return The <code>int</code> that was read
     * @throws IOException if an error occurs while reading from the
     *         underlying input stream
     */
    public int readInt() throws IOException {
        int ch1 = read();
        int ch2 = read();
        int ch3 = read();
        int ch4 = read();
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4));
    }

    /**
     * Read in a <code>short</code>.
     *
     * @return The <code>short</code> that was read
     * @throws IOException if an error occurs while reading from the
     *         underlying input stream
     */
    public int readShort() throws IOException {
        int ch1 = read();
        int ch2 = read();
        if ((ch1 | ch2) < 0)
            throw new EOFException();
        return (ch1 << 8) + (ch2);
    }

    /**
     * Read a given amount of data from the underlying input stream. The data
     * that is read is stored in <code>b</code>, starting from offset
     * <code>off</code>.
     *
     * @param b The byte buffer to hold the data that is read
     * @param off The offset at which to start storing data in <code>b</code>
     * @param len The number of bytes to be read
     * @throws IOException if an error occurs while reading from the
     *         underlying input stream
     */
    public void readFully(byte[] b, int off, int len) throws IOException {
        if (len < 0)
            throw new IndexOutOfBoundsException();
        int n = 0;
        while (n < len) {
            int count = read(b, off + n, len - n);
            if (count < 0)
                throw new EOFException();
            n += count;
        }
    }

    /**
     * Wraps the underlying stream in an {@link CipherInputStream} using the provided {@code cipher}.
     *
     * @param cipher
     *         Cipher for decrypting the stream
     * @throws IOException
     *         If the underlying stream is already wrapped, or if this stream is closed.
     */
    public synchronized void setCipher(Cipher cipher) throws IOException {
        InputStream currentStream = in;
        if (currentStream == null) {
            throw new IOException("Stream closed");
        } else if (currentStream instanceof CipherInputStream) {
            throw new IOException("Input stream already encrypted");
        }
        in = new CipherInputStream(currentStream, cipher);
    }
}
