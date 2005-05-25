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
 */

package org.firebirdsql.gds;

/**
 * The class <code>XSQLDA</code> is a java mapping of the XSQLDA server
 * data structure used to represent one row for input and output.
 *
 * @author <a href="mailto:alberola@users.sourceforge.net">Alejandro Alberola</a>
 * @version 1.0
 */
public class XSQLDA {
    public int version;
    public int sqln;
    public int sqld;
    public XSQLVAR[] sqlvar;
    public byte[] blr;
    public int[] ioLength;	 // 0 varchar, >0 char, -4 int etc, -8 long etc

    public XSQLDA() {
        version = ISCConstants.SQLDA_VERSION1;
    }

    public XSQLDA(int n) {
        version = ISCConstants.SQLDA_VERSION1;
        sqln = n;
        sqld = n;
        sqlvar = new XSQLVAR[n];
    }

}