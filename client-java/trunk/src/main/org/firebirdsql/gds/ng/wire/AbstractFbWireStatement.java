/*
 * $Id$
 *
 * Firebird Open Source J2EE Connector - JDBC Driver
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
 * can be obtained from a source repository history command.
 *
 * All rights reserved.
 */
package org.firebirdsql.gds.ng.wire;

import org.firebirdsql.gds.impl.wire.XdrInputStream;
import org.firebirdsql.gds.impl.wire.XdrOutputStream;
import org.firebirdsql.gds.ng.AbstractFbStatement;
import org.firebirdsql.gds.ng.FbTransaction;
import org.firebirdsql.gds.ng.fields.RowDescriptor;
import org.firebirdsql.jdbc.FBSQLException;

import java.sql.SQLException;
import java.sql.SQLNonTransientException;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author <a href="mailto:mrotteveel@users.sourceforge.net">Mark Rotteveel</a>
 * @since 2.3
 */
public abstract class AbstractFbWireStatement extends AbstractFbStatement implements FbWireStatement {

    private final AtomicReference<FbTransaction> transaction = new AtomicReference<FbTransaction>();
    private final Map<RowDescriptor, byte[]> blrCache = Collections.synchronizedMap(new WeakHashMap<RowDescriptor, byte[]>());
    private volatile int handle;
    private final XdrStreamHolder xdrStreamHolder;
    private FbWireDatabase database;

    public AbstractFbWireStatement(FbWireDatabase database) {
        this.database = database;
        xdrStreamHolder = new XdrStreamHolder(database);
    }

    protected final XdrInputStream getXdrIn() throws SQLException {
        return xdrStreamHolder.getXdrIn();
    }

    protected final XdrOutputStream getXdrOut() throws SQLException {
        return xdrStreamHolder.getXdrOut();
    }

    protected final FbWireDatabase getDatabase() {
        return database;
    }

    @Override
    public FbTransaction getTransaction() throws SQLException {
        return transaction.get();
    }

    @Override
    public void setTransaction(FbTransaction transaction) throws SQLException {
        if (!(transaction instanceof FbWireTransaction)) {
            throw new SQLNonTransientException(String.format("Invalid transaction handle, expected instance of FbWireTransaction, got \"%s\"", transaction.getClass().getName()),
                    FBSQLException.SQL_STATE_GENERAL_ERROR);
        }
        // TODO Needs synchronization?
        // TODO Is there a statement or transaction state where we should not be switching transactions?
        if (transaction == this.transaction.get()) return;
        this.transaction.set(transaction);
        // TODO Implement + add transaction listener
    }

    @Override
    public final int getHandle() {
        return handle;
    }

    protected final void setHandle(int handle) {
        synchronized (getSynchronizationObject()) {
            this.handle = handle;
        }
    }

    /**
     * Returns the (possibly cached) blr byte array for a {@link RowDescriptor}, or <code>null</code> if the parameter is null.
     *
     * @param rowDescriptor
     *         The row descriptor.
     * @return blr byte array or <code>null</code> when <code>rowDescriptor</code> is <code>null</code>
     * @throws SQLException
     *         When the {@link RowDescriptor} contains an unsupported field type.
     */
    protected final byte[] calculateBlr(RowDescriptor rowDescriptor) throws SQLException {
        if (rowDescriptor == null) return null;
        byte[] blr = blrCache.get(rowDescriptor);
        if (blr == null) {
            blr = getDatabase().getBlrCalculator().calculateBlr(rowDescriptor);
            blrCache.put(rowDescriptor, blr);
        }
        return blr;
    }

    public void close() throws SQLException {
        synchronized (getSynchronizationObject()) {
            try {
                super.close();
            } finally {
                database = null;
                transaction.set(null);
                blrCache.clear();
            }
        }
    }
}
