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

/* added by Blas Rodriguez Somoza:
 *
 * CVS modification log:
 * $Log$
 */

package org.firebirdsql.encodings;

public interface Encoding{

    // encode
    public abstract byte[] encodeToCharset(String in);
    public abstract int encodeToCharset(char[] in, int off, int len, byte[] out);

    // decode
    public abstract String decodeFromCharset(byte[] in);
    public abstract int decodeFromCharset(byte[] in, int off, int len, char[] out);
}