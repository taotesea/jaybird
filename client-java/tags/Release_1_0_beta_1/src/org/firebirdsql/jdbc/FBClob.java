/*   The contents of this file are subject to the Mozilla Public
 *   License Version 1.1 (the "License"); you may not use this file
 *   except in compliance with the License. You may obtain a copy of
 *   the License at http://www.mozilla.org/MPL/
 *   Alternatively, the contents of this file may be used under the
 *   terms of the GNU Lesser General Public License Version 2 or later (the
 *   "LGPL"), in which case the provisions of the GPL are applicable
 *   instead of those above. You may obtain a copy of the Licence at
 *   http://www.gnu.org/copyleft/lgpl.html
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    relevant License for more details.
 *
 *    This file was created by members of the firebird development team.
 *    All individual contributions remain the Copyright (C) of those
 *    individuals.  Contributors to this file are either listed here or
 *    can be obtained from a CVS history command.
 *
 *    All rights reserved.

 */

package org.firebirdsql.jdbc;


// imports --------------------------------------
import java.sql.Clob;
import java.sql.SQLException;
import java.io.Reader;
import java.io.OutputStream;
import java.io.Writer;
import java.io.InputStream;


/**
 *
 *   @see <related>
 *   @author David Jencks (davidjencks@earthlink.net)
 *   @version $ $
 */


/**
 * The mapping in the Java<sup><font size=-2>TM</font></sup> programming language
 * for the SQL <code>CLOB</code> type.
 * An SQL <code>CLOB</code> is a built-in type
 * that stores a Character Large Object as a column value in a row of
 * a database table.
 * The driver implements a <code>Clob</code> object using an SQL
 * <code>locator(CLOB)</code>, which means that a <code>Clob</code> object
 * contains a logical pointer to the SQL <code>CLOB</code> data rather than
 * the data itself. A <code>Clob</code> object is valid for the duration
 * of the transaction in which it was created.
 * <P>The <code>Clob</code> interface provides methods for getting the
 * length of an SQL <code>CLOB</code> (Character Large Object) value,
 * for materializing a <code>CLOB</code> value on the client, and for
 * searching for a substring or <code>CLOB</code> object within a
 * <code>CLOB</code> value.
 * Methods in the interfaces {@link ResultSet},
 * {@link CallableStatement}, and {@link PreparedStatement}, such as
 * <code>getClob</code> and <code>setClob</code> allow a programmer to
 * access an SQL <code>CLOB</code> value.
 <P>
 * This class is new in the JDBC 2.0 API.
 */

public class FBClob implements Clob {

  /**
   * Returns the number of characters
   * in the <code>CLOB</code> value
   * designated by this <code>Clob</code> object.
   * @return length of the <code>CLOB</code> in characters
   * @exception SQLException if there is an error accessing the
   * length of the <code>CLOB</code>
   * @since 1.2
   * @see <a href="package-summary.html#2.0 API">What Is in the JDBC 2.0 API</a>
   */
    public long length() throws SQLException {
        throw new SQLException("Not yet implemented");
    }

    /**
     *
     * @param param1 <description>
     * @exception java.sql.SQLException <description>
     */
    public void truncate(long param1) throws SQLException {
        // TODO: implement this java.sql.Clob method
    }



  /**
   * Returns a copy of the specified substring
   * in the <code>CLOB</code> value
   * designated by this <code>Clob</code> object.
   * The substring begins at position
   * <code>pos</code> and has up to <code>length</code> consecutive
   * characters.
   * @param pos the first character of the substring to be extracted.
   *            The first character is at position 1.
   * @param length the number of consecutive characters to be copied
   * @return a <code>String</code> that is the specified substring in
   *         the <code>CLOB</code> value designated by this <code>Clob</code> object
   * @exception SQLException if there is an error accessing the
   * <code>CLOB</code>
   * @since 1.2
   * @see <a href="package-summary.html#2.0 API">What Is in the JDBC 2.0 API</a>
   */
    public String getSubString(long pos, int length) throws SQLException {
        throw new SQLException("Not yet implemented");
    }


  /**
   * Gets the <code>CLOB</code> value designated by this <code>Clob</code>
   * object as a Unicode stream.
   * @return a Unicode stream containing the <code>CLOB</code> data
   * @exception SQLException if there is an error accessing the
   * <code>CLOB</code> value
   * @since 1.2
   * @see <a href="package-summary.html#2.0 API">What Is in the JDBC 2.0 API</a>
   */
    public Reader getCharacterStream() throws SQLException {
        throw new SQLException("Not yet implemented");
    }


  /**
   * Gets the <code>CLOB</code> value designated by this <code>Clob</code>
   * object as a stream of Ascii bytes.
   * @return an ascii stream containing the <code>CLOB</code> data
   * @exception SQLException if there is an error accessing the
   * <code>CLOB</code> value
   * @since 1.2
   * @see <a href="package-summary.html#2.0 API">What Is in the JDBC 2.0 API</a>
   */
    public InputStream getAsciiStream() throws SQLException {
        throw new SQLException("Not yet implemented");
    }


  /**
   * Determines the character position at which the specified substring
   * <code>searchstr</code> appears in the SQL <code>CLOB</code> value
   * represented by this <code>Clob</code> object.  The search
   * begins at position <code>start</code>.
   * @param searchstr the substring for which to search
   * @param start the position at which to begin searching; the first position
   *              is 1
   * @return the position at which the substring appears, else -1; the first
   *         position is 1
   * @exception SQLException if there is an error accessing the
   * <code>CLOB</code> value
   * @since 1.2
   * @see <a href="package-summary.html#2.0 API">What Is in the JDBC 2.0 API</a>
   */
    public long position(String searchstr, long start) throws SQLException {
        throw new SQLException("Not yet implemented");
    }


  /**
   * Determines the character position at which the specified
   * <code>Clob</code> object <code>searchstr</code> appears in this
   * <code>Clob</code> object.  The search begins at position
   * <code>start</code>.
   * @param searchstr the <code>Clob</code> object for which to search
   * @param start the position at which to begin searching; the first
   *              position is 1
   * @return the position at which the <code>Clob</code> object appears,
   * else -1; the first position is 1
   * @exception SQLException if there is an error accessing the
   * <code>CLOB</code> value
   * @since 1.2
   * @see <a href="package-summary.html#2.0 API">What Is in the JDBC 2.0 API</a>
   */
    public long position(Clob searchstr, long start) throws SQLException {
        throw new SQLException("Not yet implemented");
    }

    /**
     *
     * @param param1 <description>
     * @param param2 <description>
     * @return <description>
     * @exception java.sql.SQLException <description>
     */
    public int setString(long param1, String param2) throws SQLException {
        // TODO: implement this java.sql.Clob method
        throw new SQLException("not yet implemented");

    }

    /**
     *
     * @param param1 <description>
     * @param param2 <description>
     * @param param3 <description>
     * @param param4 <description>
     * @return <description>
     * @exception java.sql.SQLException <description>
     */
    public int setString(long param1, String param2, int param3, int param4) throws SQLException {
        // TODO: implement this java.sql.Clob method
        throw new SQLException("not yet implemented");

    }

    /**
     *
     * @param param1 <description>
     * @return <description>
     * @exception java.sql.SQLException <description>
     */
    public OutputStream setAsciiStream(long param1) throws SQLException {
        // TODO: implement this java.sql.Clob method
        throw new SQLException("not yet implemented");
    }

    /**
     *
     * @param param1 <description>
     * @return <description>
     * @exception java.sql.SQLException <description>
     */
    public Writer setCharacterStream(long param1) throws SQLException {
        // TODO: implement this java.sql.Clob method
        throw new SQLException("not yet implemented");
    }


}