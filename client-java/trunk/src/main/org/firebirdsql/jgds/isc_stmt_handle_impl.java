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

package org.firebirdsql.jgds;

import org.firebirdsql.gds.XSQLDA;

import java.util.List;
import java.util.ArrayList;

/**
 * Describe class <code>isc_stmt_handle_impl</code> here.
 *
 * @author <a href="mailto:alberola@users.sourceforge.net">Alejandro Alberola</a>
 * @author <a href="mailto:d_jencks@users.sourceforge.net">David Jencks</a>
 * @version 1.0
 */
public final class isc_stmt_handle_impl implements org.firebirdsql.gds.isc_stmt_handle {
    int rsr_id;
    isc_db_handle_impl rsr_rdb;
    XSQLDA in_sqlda = null;
    XSQLDA out_sqlda = null;
    public Object[] rows;
	 public int size;
    public boolean allRowsFetched = false;
    boolean isSingletonResult = false;

    int statementType;
    int insertCount;
    int updateCount;
    int deleteCount;
    int selectCount; //????

    public isc_stmt_handle_impl() {
    }

    public XSQLDA getInSqlda() {
        return in_sqlda;
    }

    public XSQLDA getOutSqlda() {
        return out_sqlda;
    }
	 
    public void ensureCapacity(int maxSize) {
        if (rows== null || rows.length<maxSize)
            rows = new Object[maxSize];
        size=0;
    }

    public void clearRows() {
        size = 0;
        allRowsFetched = false;
    }

    public int getStatementType() {
        return statementType;
    }

    public int getInsertCount() {
        return insertCount;
    }

    public int getUpdateCount() {
        return updateCount;
    }

    public int getDeleteCount() {
        return deleteCount;
    }

    public int getSelectCount() {
        return selectCount;
    }
}
