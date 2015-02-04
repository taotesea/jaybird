/*
 * $Id$
 * 
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
package org.firebirdsql.gds.impl.jni;

import java.io.UnsupportedEncodingException;

import org.firebirdsql.encodings.EncodingFactory;
import org.firebirdsql.gds.*;
import org.firebirdsql.gds.impl.AbstractGDS;
import org.firebirdsql.gds.impl.AbstractIscTrHandle;
import org.firebirdsql.gds.impl.DatabaseParameterBufferExtension;
import org.firebirdsql.gds.impl.GDSType;
import org.firebirdsql.logging.Logger;
import org.firebirdsql.logging.LoggerFactory;

public abstract class BaseGDSImpl extends AbstractGDS {

    private static Logger log = LoggerFactory.getLogger(BaseGDSImpl.class,
            false);
    
    private static final String WARNING_CONNECT_TIMEOUT_NATIVE = 
            "WARNING: The native driver does not apply connectTimeout for establishing the socket connection (only for protocol negotiation with the Firebird server), " + 
            "it will not detect unreachable hosts within the specified timeout";
   
    protected static final byte[] DESCRIBE_DATABASE_INFO_BLOCK = new byte[] {
        ISCConstants.isc_info_db_sql_dialect,
        ISCConstants.isc_info_firebird_version,
        ISCConstants.isc_info_ods_version,
        ISCConstants.isc_info_ods_minor_version,
        ISCConstants.isc_info_implementation,
        ISCConstants.isc_info_db_class, 
        ISCConstants.isc_info_base_level,
        ISCConstants.isc_info_end };


    private static byte[] stmtInfo = new byte[] {
                ISCConstants.isc_info_sql_records,
                ISCConstants.isc_info_sql_stmt_type, ISCConstants.isc_info_end};

    private static int INFO_SIZE = 128;

    public int isc_api_handle;
    
    public BaseGDSImpl() {
        super();
    }

    public BaseGDSImpl(GDSType gdsType) {
        super(gdsType);
    }

    protected abstract String getServerUrl(String file_name)
            throws GDSException;

    public BlobParameterBuffer createBlobParameterBuffer() {
        return new BlobParameterBufferImp();
    }

    public DatabaseParameterBuffer createDatabaseParameterBuffer() {
        return new DatabaseParameterBufferImp();
    }

    public IscBlobHandle createIscBlobHandle() {
        return new isc_blob_handle_impl();
    }

    // Handle declaration methods
    public IscDbHandle createIscDbHandle() {
        return new isc_db_handle_impl();
    }

    public IscStmtHandle createIscStmtHandle() {
        return new isc_stmt_handle_impl();
    }

    public IscSvcHandle createIscSvcHandle() {
        return new isc_svc_handle_impl();
    }

    public IscTrHandle createIscTrHandle() {
        return new isc_tr_handle_impl();
    }

    // GDS Implementation
    // ----------------------------------------------------------------------------------------------

    public ServiceParameterBuffer createServiceParameterBuffer() {
        return new ServiceParameterBufferImp();
    }

    public ServiceRequestBuffer createServiceRequestBuffer(int taskIdentifier) {
        return new ServiceRequestBufferImp(taskIdentifier);
    }

    // isc_attach_database
    // ---------------------------------------------------------------------------------------------
    public void iscAttachDatabase(String file_name, IscDbHandle db_handle,
            DatabaseParameterBuffer databaseParameterBuffer)
            throws GDSException {
        validateHandle(db_handle);

        final byte[] dpbBytes;
        final String filenameCharset;
        if (databaseParameterBuffer != null) {
            DatabaseParameterBuffer cleanDPB = ((DatabaseParameterBufferExtension)databaseParameterBuffer).removeExtensionParams();
            if (cleanDPB.hasArgument(DatabaseParameterBuffer.CONNECT_TIMEOUT)) {
                // For the native driver isc_dpb_connect_timeout is not a socket connect timeout
                // It only applies to the steps for op_accept (negotiating protocol, etc)
                if (log != null) {
                    log.warn(WARNING_CONNECT_TIMEOUT_NATIVE);
                }
                db_handle.addWarning(new GDSWarning(WARNING_CONNECT_TIMEOUT_NATIVE));
            }
            if (!cleanDPB.hasArgument(DatabaseParameterBuffer.SQL_DIALECT)) {
                cleanDPB.addArgument(DatabaseParameterBuffer.SQL_DIALECT, ISCConstants.SQL_DIALECT_CURRENT);
            }
            
            dpbBytes = ((DatabaseParameterBufferImp) cleanDPB).getBytesForNativeCode();
            filenameCharset = databaseParameterBuffer.getArgumentAsString(DatabaseParameterBufferExtension.FILENAME_CHARSET);
        } else {
            dpbBytes = null;
            filenameCharset = null;
        }

        String serverUrl = getServerUrl(file_name);
        
        byte[] urlData;
        try {
            if (filenameCharset != null)
                urlData = serverUrl.getBytes(filenameCharset);
            else
                urlData = serverUrl.getBytes();
            
            byte[] nullTerminated = new byte[urlData.length + 1];
            System.arraycopy(urlData, 0, nullTerminated, 0, urlData.length);
            urlData = nullTerminated;
        } catch(UnsupportedEncodingException ex) {
            throw new GDSException(ISCConstants.isc_bad_dpb_content);
        }
        
        synchronized (db_handle) {
            native_isc_attach_database(urlData, db_handle, dpbBytes);
        }

        parseAttachDatabaseInfo(iscDatabaseInfo(db_handle,
                DESCRIBE_DATABASE_INFO_BLOCK, 1024), db_handle);
    }

    public byte[] iscBlobInfo(IscBlobHandle handle, byte[] items,
            int buffer_length) throws GDSException {
        isc_blob_handle_impl blob = validateHandle(handle);
        final isc_db_handle_impl db = validateHandle(blob.getDb());

        synchronized (db) {
            return native_isc_blob_info(blob, items, buffer_length);
        }
    }

    // isc_close_blob
    // ---------------------------------------------------------------------------------------------
    public void iscCloseBlob(IscBlobHandle blob_handle) throws GDSException {
        isc_blob_handle_impl blob = validateHandle(blob_handle);
        isc_db_handle_impl db = validateHandle(blob.getDb());
        isc_tr_handle_impl tr = validateHandle(blob.getTr());

        synchronized (db) {
            native_isc_close_blob(blob_handle);
        }

        tr.removeBlob(blob);
    }

    // isc_commit_retaining
    // ---------------------------------------------------------------------------------------------
    public void iscCommitRetaining(IscTrHandle tr_handle) throws GDSException {
        isc_tr_handle_impl tr = validateHandle(tr_handle);
        isc_db_handle_impl db = validateHandle(tr.getDbHandle());

        synchronized (db) {
            if (tr.getState() != AbstractIscTrHandle.TRANSACTIONSTARTED
                    && tr.getState() != AbstractIscTrHandle.TRANSACTIONPREPARED) { throw new GDSException(
                    ISCConstants.isc_tra_state); }

            tr.setState(AbstractIscTrHandle.TRANSACTIONCOMMITTING);

            native_isc_commit_retaining(tr_handle);

            tr.setState(AbstractIscTrHandle.TRANSACTIONSTARTED);
        }
    }

    // isc_commit_transaction
    // ---------------------------------------------------------------------------------------------
    public void iscCommitTransaction(IscTrHandle tr_handle) throws GDSException {
        isc_tr_handle_impl tr = validateHandle(tr_handle);
        isc_db_handle_impl db = validateHandle(tr.getDbHandle());

        synchronized (db) {
            if (tr.getState() != AbstractIscTrHandle.TRANSACTIONSTARTED
                    && tr.getState() != AbstractIscTrHandle.TRANSACTIONPREPARED) { throw new GDSException(
                    ISCConstants.isc_tra_state); }

            tr.setState(AbstractIscTrHandle.TRANSACTIONCOMMITTING);

            native_isc_commit_transaction(tr_handle);

            tr.setState(AbstractIscTrHandle.NOTRANSACTION);

            tr.unsetDbHandle();
        }
    }

    // isc_create_blob2
    // ---------------------------------------------------------------------------------------------
    public void iscCreateBlob2(IscDbHandle db_handle, IscTrHandle tr_handle,
            IscBlobHandle blob_handle, BlobParameterBuffer blobParameterBuffer)
            throws GDSException {
        isc_db_handle_impl db = validateHandle(db_handle);
        isc_tr_handle_impl tr = validateHandle(tr_handle);
        isc_blob_handle_impl blob = validateHandle(blob_handle);

        final byte[] bpb = blobParameterBuffer == null ? null
                : ((BlobParameterBufferImp) blobParameterBuffer)
                        .getBytesForNativeCode();

        synchronized (db) {
            native_isc_create_blob2(db_handle, tr_handle, blob_handle, bpb);

            blob.setDb(db);
            blob.setTr(tr);
            tr.addBlob(blob);
        }
    }

    // isc_create_database
    // ---------------------------------------------------------------------------------------------
    public void iscCreateDatabase(String file_name, IscDbHandle db_handle,
            DatabaseParameterBuffer dpb)
            throws GDSException {
        validateHandle(db_handle);

        final byte[] dpbBytes;
        final String filenameCharset;
        if (dpb != null) {
            DatabaseParameterBuffer cleanDPB = ((DatabaseParameterBufferExtension)dpb).removeExtensionParams();
            if (cleanDPB.hasArgument(DatabaseParameterBuffer.CONNECT_TIMEOUT)) {
                // For the native driver isc_dpb_connect_timeout is not a socket connect timeout
                // It only applies to the steps for op_accept (negotiating protocol, etc)
                if (log != null) {
                    log.warn(WARNING_CONNECT_TIMEOUT_NATIVE);
                }
                db_handle.addWarning(new GDSWarning(WARNING_CONNECT_TIMEOUT_NATIVE));
            }
            if (!cleanDPB.hasArgument(DatabaseParameterBuffer.SQL_DIALECT)) {
                cleanDPB.addArgument(DatabaseParameterBuffer.SQL_DIALECT, ISCConstants.SQL_DIALECT_CURRENT);
            }

            dpbBytes = ((DatabaseParameterBufferImp) cleanDPB).getBytesForNativeCode();
            filenameCharset = dpb.getArgumentAsString(DatabaseParameterBufferExtension.FILENAME_CHARSET);
        } else {
            dpbBytes = null;
            filenameCharset = null;
        }

        synchronized (db_handle) {
            String serverUrl  = getServerUrl(file_name);
            
            byte[] urlData;
            try {
                if (filenameCharset != null)
                    urlData = serverUrl.getBytes(filenameCharset);
                else
                    urlData = serverUrl.getBytes();
                
                byte[] nullTerminated = new byte[urlData.length + 1];
                System.arraycopy(urlData, 0, nullTerminated, 0, urlData.length);
                urlData = nullTerminated;

            } catch(UnsupportedEncodingException ex) {
                throw new GDSException(ISCConstants.isc_bad_dpb_content);
            }
            
            native_isc_create_database(urlData, db_handle,
                    dpbBytes);
        }
    }

    // isc_attach_database
    // ---------------------------------------------------------------------------------------------
    public byte[] iscDatabaseInfo(IscDbHandle db_handle, byte[] items,
            int buffer_length) throws GDSException {
        validateHandle(db_handle);

        synchronized (db_handle) {
            final byte[] returnValue = new byte[buffer_length];

            native_isc_database_info(db_handle, items.length, items,
                    buffer_length, returnValue);

            return returnValue;
        }
    }

    // isc_detach_database
    // ---------------------------------------------------------------------------------------------
    public void iscDetachDatabase(IscDbHandle db_handle) throws GDSException {
        isc_db_handle_impl db = validateHandle(db_handle);

        synchronized (db) {
//            if (db.hasTransactions()) { throw new GDSException(
//                    ISCConstants.isc_open_trans, db.getOpenTransactionCount()); }

            native_isc_detach_database(db_handle);
            db.invalidate();
        }
    }

    // isc_drop_database
    // ---------------------------------------------------------------------------------------------
    public void iscDropDatabase(IscDbHandle db_handle) throws GDSException {
        validateHandle(db_handle);

        synchronized (db_handle) {
            native_isc_drop_database(db_handle);
        }
    }

    // isc_dsql_allocate_statement
    // ---------------------------------------------------------------------------------------------
    public void iscDsqlAllocateStatement(IscDbHandle db_handle,
            IscStmtHandle stmt_handle) throws GDSException {
        isc_db_handle_impl db = validateHandle(db_handle);
        isc_stmt_handle_impl stmt = validateHandle(stmt_handle);

        synchronized (db) {
            native_isc_dsql_allocate_statement(db_handle, stmt_handle);

            stmt.setRsr_rdb((isc_db_handle_impl) db_handle);
            stmt.setAllRowsFetched(false);
        }
    }

    // isc_dsql_describe
    // ---------------------------------------------------------------------------------------------
    public XSQLDA iscDsqlDescribe(IscStmtHandle stmt_handle, int da_version)
            throws GDSException {
        isc_stmt_handle_impl stmt = validateHandle(stmt_handle);
        final isc_db_handle_impl db = validateHandle(stmt.getRsr_rdb());

        synchronized (db) {
            // TODO setInSqlda here ??
            stmt.setInSqlda(native_isc_dsql_describe(stmt_handle, da_version));

            return stmt_handle.getInSqlda();
        }
    }

    // isc_dsql_describe_bind
    // ---------------------------------------------------------------------------------------------
    public XSQLDA iscDsqlDescribeBind(IscStmtHandle stmt_handle, int da_version)
            throws GDSException {
        isc_stmt_handle_impl stmt = validateHandle(stmt_handle);
        final isc_db_handle_impl db = validateHandle(stmt.getRsr_rdb());

        synchronized (db) {
            stmt.setInSqlda(native_isc_dsql_describe_bind(stmt_handle, da_version));

            return stmt_handle.getInSqlda();
        }
    }

    public void iscDsqlExecImmed2(IscDbHandle db_handle, IscTrHandle tr_handle,
            byte[] statement, int dialect, XSQLDA in_xsqlda, XSQLDA out_xsqlda)
            throws GDSException {
        validateHandle(db_handle);

        synchronized (db_handle) {
            native_isc_dsql_exec_immed2(db_handle, tr_handle,
                    getZeroTerminatedArray(statement), dialect, in_xsqlda,
                    out_xsqlda);
        }
    }

    public void iscDsqlExecImmed2(IscDbHandle db_handle, IscTrHandle tr_handle,
            String statement, int dialect, XSQLDA in_xsqlda, XSQLDA out_xsqlda)
            throws GDSException {
        iscDsqlExecImmed2(db_handle, tr_handle, statement, "NONE", dialect,
                in_xsqlda, out_xsqlda);
    }

    public void iscDsqlExecImmed2(IscDbHandle db_handle, IscTrHandle tr_handle,
            String statement, String encoding, int dialect, XSQLDA in_xsqlda,
            XSQLDA out_xsqlda) throws GDSException {
        validateHandle(db_handle);

        try {
            synchronized (db_handle) {
                native_isc_dsql_exec_immed2(db_handle, tr_handle,
                        getByteArrayForString(statement, encoding), dialect,
                        in_xsqlda, out_xsqlda);
            }
        } catch (UnsupportedEncodingException e) {
            throw new GDSException("Unsupported encoding. " + e.getMessage());
        }
    }

    // isc_dsql_execute
    // ---------------------------------------------------------------------------------------------
    public void iscDsqlExecute(IscTrHandle tr_handle,
            IscStmtHandle stmt_handle, int da_version, XSQLDA xsqlda)
            throws GDSException {
        iscDsqlExecute2(tr_handle, stmt_handle, da_version, xsqlda, null);
    }

    // public synchronized abstract void native_isc_dsql_execute(isc_tr_handle
    // tr_handle, isc_stmt_handle stmt_handle, int da_version, XSQLDA xsqlda)
    // throws GDSException;

    // isc_dsql_execute2
    // ---------------------------------------------------------------------------------------------
    public void iscDsqlExecute2(IscTrHandle tr_handle,
            IscStmtHandle stmt_handle, int da_version, XSQLDA in_xsqlda,
            XSQLDA out_xsqlda) throws GDSException {
        isc_stmt_handle_impl stmt = validateHandle(stmt_handle);
        final isc_db_handle_impl db = validateHandle(stmt.getRsr_rdb());

        synchronized (db) {
            // TODO Fetch Statements
            native_isc_dsql_execute2(tr_handle, stmt_handle, da_version,
                    in_xsqlda, out_xsqlda);

            if (stmt.getOutSqlda() != null) stmt.notifyOpenResultSet();

            if (out_xsqlda != null) {
                // this would be an Execute procedure
                stmt.ensureCapacity(1);
                readSQLData(out_xsqlda, stmt);
                stmt.setAllRowsFetched(true);
                stmt.setSingletonResult(true);
            } else {
                stmt.setAllRowsFetched(false);
                stmt.setSingletonResult(false);
            }
            
            stmt.registerTransaction((AbstractIscTrHandle)tr_handle);
        }
    }

    public void iscDsqlExecuteImmediate(IscDbHandle db_handle,
            IscTrHandle tr_handle, byte[] statement, int dialect, XSQLDA xsqlda)
            throws GDSException {

        iscDsqlExecImmed2(db_handle, tr_handle, statement, dialect, xsqlda,
                null);
    }

    // isc_dsql_execute_immediateX
    // ---------------------------------------------------------------------------------------------
    public void iscDsqlExecuteImmediate(IscDbHandle db_handle,
            IscTrHandle tr_handle, String statement, int dialect, XSQLDA xsqlda)
            throws GDSException {
        iscDsqlExecImmed2(db_handle, tr_handle, statement, dialect, xsqlda,
                null);
    }

    public void iscDsqlExecuteImmediate(IscDbHandle db_handle,
            IscTrHandle tr_handle, String statement, String encoding,
            int dialect, XSQLDA xsqlda) throws GDSException {
        iscDsqlExecImmed2(db_handle, tr_handle, statement, encoding, dialect,
                xsqlda, null);
    }

    // isc_dsql_fetch
    // ---------------------------------------------------------------------------------------------
    public void iscDsqlFetch(IscStmtHandle stmt_handle, int da_version,
            XSQLDA xsqlda, int fetchSize) throws GDSException {
        fetchSize = 1;

        if (xsqlda == null) { 
            throw new GDSException(ISCConstants.isc_dsql_sqlda_err);
        }
        // TODO: Above declares fetchSize = 1, so parameter is ignored and this check is not necessary
        if (fetchSize <= 0) { 
            throw new GDSException(ISCConstants.isc_dsql_sqlda_err);
        }

        isc_stmt_handle_impl stmt = validateHandle(stmt_handle);
        isc_db_handle_impl db = validateHandle(stmt.getRsr_rdb());

        synchronized (db) {
            // Apply fetchSize
            // Fetch next batch of rows
            stmt.ensureCapacity(fetchSize);

            for (int i = 0; i < fetchSize; i++) {
                try {
                    boolean isRowPresent = native_isc_dsql_fetch(stmt_handle,
                            da_version, xsqlda, fetchSize);
                    if (isRowPresent) {
                        readSQLData(xsqlda, stmt);
                    } else {
                        stmt.setAllRowsFetched(true);
                        return;
                    }
                } finally {
                    stmt.notifyOpenResultSet();
                }
            }
        }
    }

    // isc_dsql_free_statement
    // ---------------------------------------------------------------------------------------------
    public void iscDsqlFreeStatement(IscStmtHandle stmt_handle, int option)
            throws GDSException {
        isc_stmt_handle_impl stmt = validateHandle(stmt_handle);
        isc_db_handle_impl db = validateHandle(stmt.getRsr_rdb());

        synchronized (db) {
            // Does not seem to be possible or necessary to close
            // an execute procedure statement.
            if (stmt.isSingletonResult() && option == ISCConstants.DSQL_close) { return; }

            if (option == ISCConstants.DSQL_drop) {
                stmt.setInSqlda(null);
                stmt.setOutSqlda(null);
                stmt.setRsr_rdb(null);
            }

            native_isc_dsql_free_statement(stmt_handle, option);
            
            // clear association with transaction
            try {
                AbstractIscTrHandle tr = stmt.getTransaction();
                if (tr != null)
                    tr.unregisterStatementFromTransaction(stmt);
            } finally {
                stmt.unregisterTransaction();
            }

        }
    }

    // isc_dsql_free_statement
    // ---------------------------------------------------------------------------------------------
    public XSQLDA iscDsqlPrepare(IscTrHandle tr_handle,
            IscStmtHandle stmt_handle, byte[] statement, int dialect)
            throws GDSException {

        validateHandle(tr_handle);
        isc_stmt_handle_impl stmt = validateHandle(stmt_handle);
        isc_db_handle_impl db = validateHandle(stmt.getRsr_rdb());

        synchronized (db) {
            stmt.setInSqlda(null);
            stmt.setOutSqlda(null);

            stmt.setOutSqlda(native_isc_dsql_prepare(tr_handle, stmt_handle,
                    getZeroTerminatedArray(statement), dialect));

            getStatementType(stmt);
            
            return stmt_handle.getOutSqlda();
        }
    }

    /**
     * Find out the type of the specified statement.
     * 
     * @param stmt instance of {@link isc_stmt_handle_impl}.
     * 
     * @throws GDSException if error occured.
     */
    private void getStatementType(isc_stmt_handle_impl stmt) throws GDSException {
        final byte [] REQUEST = new byte [] {
            ISCConstants.isc_info_sql_stmt_type,
            ISCConstants.isc_info_end };

        int bufferSize = 1024;
        byte[] buffer;
        
        buffer = iscDsqlSqlInfo(stmt, REQUEST, bufferSize); 

        /*
        if (buffer[0] == ISCConstants.isc_info_end){
            throw new GDSException("Statement info could not be retrieved");
        }
        */

        int dataLength = -1; 
        for (int i = 0; i < buffer.length; i++){
            switch(buffer[i]){
                case ISCConstants.isc_info_sql_stmt_type:
                    dataLength = iscVaxInteger(buffer, ++i, 2);
                    i += 2;
                    stmt.setStatementType(iscVaxInteger(buffer, i, dataLength));
                    i += dataLength;
                    break;
                case ISCConstants.isc_info_end:
                case 0:
                    break;
                default:
                    throw new GDSException("Unknown data block [" 
                            + buffer[i] + "]");
            }
        }
    }

    // isc_dsql_free_statement
    // ---------------------------------------------------------------------------------------------
    public XSQLDA iscDsqlPrepare(IscTrHandle tr_handle,
            IscStmtHandle stmt_handle, String statement, int dialect)
            throws GDSException {
        return iscDsqlPrepare(tr_handle, stmt_handle, statement, "NONE",
                dialect);
    }

    public XSQLDA iscDsqlPrepare(IscTrHandle tr_handle,
            IscStmtHandle stmt_handle, String statement, String encoding,
            int dialect) throws GDSException {
        try {
            return iscDsqlPrepare(tr_handle, stmt_handle,
                    getByteArrayForString(statement, encoding), dialect);
        } catch (UnsupportedEncodingException e) {
            throw new GDSException("Unsupported encoding. " + e.getMessage());
        }
    }

    // isc_dsql_free_statement
    // ---------------------------------------------------------------------------------------------
    public void iscDsqlSetCursorName(IscStmtHandle stmt_handle,
            String cursor_name, int type) throws GDSException {

        isc_stmt_handle_impl stmt = validateHandle(stmt_handle);
        isc_db_handle_impl db = validateHandle(stmt.getRsr_rdb());

        synchronized (db) {
            native_isc_dsql_set_cursor_name(stmt_handle, cursor_name, type);
        }
    }

    // isc_dsql_sql_info
    // ---------------------------------------------------------------------------------------------
    public byte[] iscDsqlSqlInfo(IscStmtHandle stmt_handle, byte[] items,
            int buffer_length) throws GDSException {
        final isc_stmt_handle_impl stmt = validateHandle(stmt_handle);
        final isc_db_handle_impl db = validateHandle(stmt.getRsr_rdb());

        synchronized (db) {
            return native_isc_dsql_sql_info(stmt_handle, items, buffer_length);
        }
    }

    // isc_expand_dpb
    // ---------------------------------------------------------------------------------------------
    public byte[] iscExpandDpb(byte[] dpb, int dpb_length, int param,
            Object[] params) throws GDSException {
        return dpb;
    }

    // isc_get_segment
    // ---------------------------------------------------------------------------------------------
    public byte[] iscGetSegment(IscBlobHandle blob, int maxread)
            throws GDSException {
        final isc_blob_handle_impl blb = validateHandle(blob);
        final isc_db_handle_impl db = validateHandle(blb.getDb());

        synchronized (db) {
            return native_isc_get_segment(blob, maxread);
        }
    }

    // isc_open_blob2
    // ---------------------------------------------------------------------------------------------
    public void iscOpenBlob2(IscDbHandle db_handle, IscTrHandle tr_handle,
            IscBlobHandle blob_handle, BlobParameterBuffer blobParameterBuffer)
            throws GDSException {
        isc_db_handle_impl db = validateHandle(db_handle);
        isc_tr_handle_impl tr = validateHandle(tr_handle);
        isc_blob_handle_impl blob = validateHandle(blob_handle);

        final byte[] bpb = blobParameterBuffer == null ? null
                : ((BlobParameterBufferImp) blobParameterBuffer)
                        .getBytesForNativeCode();

        synchronized (db) {
            native_isc_open_blob2(db_handle, tr_handle, blob_handle, bpb);

            blob.setDb(db);
            blob.setTr(tr);
            tr.addBlob(blob);
        }
    }

    // isc_prepare_transaction
    // ---------------------------------------------------------------------------------------------
    public void iscPrepareTransaction(IscTrHandle tr_handle)
            throws GDSException {
        isc_tr_handle_impl tr = validateHandle(tr_handle);
        isc_db_handle_impl db = validateHandle(tr.getDbHandle());

        synchronized (db) {
            if (tr.getState() != AbstractIscTrHandle.TRANSACTIONSTARTED) {
                throw new GDSException(ISCConstants.isc_tra_state);
            }
            tr.setState(AbstractIscTrHandle.TRANSACTIONPREPARING);

            native_isc_prepare_transaction(tr_handle);

            tr.setState(AbstractIscTrHandle.TRANSACTIONPREPARED);
        }
    }

    // isc_prepare_transaction2
    // ---------------------------------------------------------------------------------------------
    public void iscPrepareTransaction2(IscTrHandle tr_handle, byte[] bytes)
            throws GDSException {
        isc_tr_handle_impl tr = validateHandle(tr_handle);
        isc_db_handle_impl db = validateHandle(tr.getDbHandle());

        synchronized (db) {
            if (tr.getState() != AbstractIscTrHandle.TRANSACTIONSTARTED) {
                throw new GDSException(ISCConstants.isc_tra_state);
            }
            tr.setState(AbstractIscTrHandle.TRANSACTIONPREPARING);

            native_isc_prepare_transaction2(tr_handle, bytes);

            tr.setState(AbstractIscTrHandle.TRANSACTIONPREPARED);
        }
    }

    // isc_put_segment
    // ---------------------------------------------------------------------------------------------
    public void iscPutSegment(IscBlobHandle blob_handle, byte[] buffer)
            throws GDSException {
        final isc_blob_handle_impl blob = validateHandle(blob_handle);
        final isc_db_handle_impl db = validateHandle(blob.getDb());

        synchronized (db) {
            native_isc_put_segment(blob_handle, buffer);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.firebirdsql.gds.GDS#isc_reconnect_transaction(org.firebirdsql.gds.isc_tr_handle,
     *      org.firebirdsql.gds.isc_db_handle, byte[])
     */
    public void iscReconnectTransaction(IscTrHandle tr_handle,
            IscDbHandle db_handle, long transactionId) throws GDSException {
        validateHandle(db_handle);
        isc_tr_handle_impl tr = validateHandle(tr_handle);
        
        byte[] buffer = new byte[4];
        for (int i = 0; i < 4; i++){
            buffer[i] = (byte)(transactionId >>> (i * 8));
        }

        synchronized (db_handle) {
            tr.setState(AbstractIscTrHandle.TRANSACTIONSTARTING);

            native_isc_reconnect_transaction(db_handle, tr_handle, buffer);
            tr.setDbHandle((isc_db_handle_impl) db_handle);

            tr.setState(AbstractIscTrHandle.TRANSACTIONSTARTED);
        }
    }

    // isc_rollback_retaining
    // ---------------------------------------------------------------------------------------------
    public void iscRollbackRetaining(IscTrHandle tr_handle) throws GDSException {
        isc_tr_handle_impl tr = validateHandle(tr_handle);
        isc_db_handle_impl db = validateHandle(tr.getDbHandle());

        synchronized (db) {
            if (tr.getState() != AbstractIscTrHandle.TRANSACTIONSTARTED
                    && tr.getState() != AbstractIscTrHandle.TRANSACTIONPREPARED) {
                throw new GDSException(ISCConstants.isc_tra_state);
            }
            tr.setState(AbstractIscTrHandle.TRANSACTIONROLLINGBACK);

            native_isc_rollback_retaining(tr_handle);

            tr.setState(AbstractIscTrHandle.TRANSACTIONSTARTED);
        }
    }

    // isc_rollback_transaction
    // ---------------------------------------------------------------------------------------------
    public void iscRollbackTransaction(IscTrHandle tr_handle)
            throws GDSException {
        isc_tr_handle_impl tr = validateHandle(tr_handle);
        isc_db_handle_impl db = validateHandle(tr.getDbHandle());

        synchronized (db) {
            if (tr.getState() == AbstractIscTrHandle.NOTRANSACTION) {
                throw new GDSException(ISCConstants.isc_tra_state);
            }

            tr.setState(AbstractIscTrHandle.TRANSACTIONROLLINGBACK);

            native_isc_rollback_transaction(tr_handle);

            tr.setState(AbstractIscTrHandle.NOTRANSACTION);
            tr.unsetDbHandle();
        }
    }


    public byte [] iscTransactionInformation(IscTrHandle trHandle, 
            byte [] requestBuffer, int bufferLen) throws GDSException {
        final IscDbHandle db = validateHandle(trHandle.getDbHandle());

        synchronized (db) {
            return native_isc_transaction_info(trHandle, requestBuffer, bufferLen);
        }
    }

    public void iscSeekBlob(IscBlobHandle handle, int position, int mode)
            throws GDSException {
        isc_blob_handle_impl blob = validateHandle(handle);
        final isc_db_handle_impl db = validateHandle(blob.getDb());

        synchronized (db) {
            native_isc_seek_blob(blob, position, mode);
        }
    }

    // Services API

    public void iscServiceAttach(String service, IscSvcHandle serviceHandle,
            ServiceParameterBuffer serviceParameterBuffer) throws GDSException {

        if (serviceHandle == null) {
            throw new GDSException(ISCConstants.isc_bad_svc_handle);
        }
        final ServiceParameterBufferImp serviceParameterBufferImp = (ServiceParameterBufferImp) serviceParameterBuffer;
        final byte[] serviceParameterBufferBytes = serviceParameterBufferImp == null ? null
                : serviceParameterBufferImp.toByteArray();

        synchronized (serviceHandle) {
            if (serviceHandle.isValid())
                throw new GDSException("serviceHandle is already attached.");

            native_isc_service_attach(service, serviceHandle,
                    serviceParameterBufferBytes);
        }
    }

    public void iscServiceDetach(IscSvcHandle serviceHandle)
            throws GDSException {
        validateHandle(serviceHandle);
        synchronized (serviceHandle) {
            native_isc_service_detach(serviceHandle);
        }
    }

    public void iscServiceQuery(IscSvcHandle serviceHandle,
            ServiceParameterBuffer serviceParameterBuffer,
            ServiceRequestBuffer serviceRequestBuffer, byte[] resultBuffer)
            throws GDSException {
        validateHandle(serviceHandle);
        final ServiceParameterBufferImp serviceParameterBufferImp = (ServiceParameterBufferImp) serviceParameterBuffer;
        final byte[] serviceParameterBufferBytes = serviceParameterBufferImp == null ? null
                : serviceParameterBufferImp.toByteArray();

        final ServiceRequestBufferImp serviceRequestBufferImp = (ServiceRequestBufferImp) serviceRequestBuffer;
        final byte[] serviceRequestBufferBytes = serviceRequestBufferImp == null ? null
                : serviceRequestBufferImp.toByteArray();

        synchronized (serviceHandle) {
            native_isc_service_query(serviceHandle,
                    serviceParameterBufferBytes, serviceRequestBufferBytes,
                    resultBuffer);
        }
    }

    public void iscServiceStart(IscSvcHandle serviceHandle,
            ServiceRequestBuffer serviceRequestBuffer) throws GDSException {
        validateHandle(serviceHandle);
        final ServiceRequestBufferImp serviceRequestBufferImp = (ServiceRequestBufferImp) serviceRequestBuffer;
        final byte[] serviceRequestBufferBytes = serviceRequestBufferImp == null ? null
                : serviceRequestBufferImp.toByteArray();

        synchronized (serviceHandle) {
            native_isc_service_start(serviceHandle, serviceRequestBufferBytes);
        }
    }

    // isc_start_transaction
    // ---------------------------------------------------------------------------------------------
    public void iscStartTransaction(IscTrHandle tr_handle,
            IscDbHandle db_handle, TransactionParameterBuffer tpb)
            throws GDSException {
        isc_tr_handle_impl tr = validateHandle(tr_handle);
        isc_db_handle_impl db = validateHandle(db_handle);
        TransactionParameterBufferImpl tpbImpl = (TransactionParameterBufferImpl) tpb;

        synchronized (db) {
            if (tr.getState() != AbstractIscTrHandle.NOTRANSACTION)
                throw new GDSException(ISCConstants.isc_tra_state);

            tr.setState(AbstractIscTrHandle.TRANSACTIONSTARTING);

            // final byte[] arg = new byte[tpb.length + 1];
            // arg[0] = 3;
            // System.arraycopy(tpb, 0, arg, 1, tpb.length);

            byte[] arg = tpbImpl.getBytesForNativeCode();
            native_isc_start_transaction(tr_handle, db_handle, arg);

            tr.setDbHandle((isc_db_handle_impl) db_handle);

            tr.setState(AbstractIscTrHandle.TRANSACTIONSTARTED);
        }
    }

    // isc_vax_integer
    // ---------------------------------------------------------------------------------------------
    public int iscVaxInteger(byte[] buffer, int pos, int length) {
        int value;
        int shift;

        value = shift = 0;

        int i = pos;
        while (--length >= 0) {
            value += (buffer[i++] & 0xff) << shift;
            shift += 8;
        }
        return value;
    }
    
    public int iscVaxInteger2(byte[] buffer, int pos) {
        return (buffer[pos] & 0xff) | ((buffer[pos + 1] & 0xff) << 8);
    }
    
    public abstract void native_isc_attach_database(byte[] file_name,
            IscDbHandle db_handle, byte[] dpbBytes);

    public abstract byte[] native_isc_blob_info(isc_blob_handle_impl handle,
            byte[] items, int buffer_length) throws GDSException;

    public abstract void native_isc_close_blob(IscBlobHandle blob)
            throws GDSException;

    public abstract void native_isc_commit_retaining(IscTrHandle tr_handle)
            throws GDSException;

    public abstract void native_isc_commit_transaction(IscTrHandle tr_handle)
            throws GDSException;

    public abstract void native_isc_create_blob2(IscDbHandle db,
            IscTrHandle tr, IscBlobHandle blob, byte[] dpbBytes);

    public abstract void native_isc_create_database(byte[] file_name,
            IscDbHandle db_handle, byte[] dpbBytes);

    public abstract void native_isc_database_info(IscDbHandle db_handle,
            int item_length, byte[] items, int buffer_length, byte[] buffer)
            throws GDSException;

    public abstract void native_isc_detach_database(IscDbHandle db_handle)
            throws GDSException;

    public abstract void native_isc_drop_database(IscDbHandle db_handle)
            throws GDSException;

    public abstract void native_isc_dsql_alloc_statement2(
            IscDbHandle db_handle, IscStmtHandle stmt_handle)
            throws GDSException;

    public abstract void native_isc_dsql_allocate_statement(
            IscDbHandle db_handle, IscStmtHandle stmt_handle)
            throws GDSException;

    public abstract XSQLDA native_isc_dsql_describe(IscStmtHandle stmt_handle,
            int da_version) throws GDSException;

    public abstract XSQLDA native_isc_dsql_describe_bind(
            IscStmtHandle stmt_handle, int da_version) throws GDSException;

    public abstract void native_isc_dsql_exec_immed2(IscDbHandle db_handle,
            IscTrHandle tr_handle, byte[] statement, int dialect,
            XSQLDA in_xsqlda, XSQLDA out_xsqlda) throws GDSException;

    public abstract void native_isc_dsql_execute2(IscTrHandle tr_handle,
            IscStmtHandle stmt_handle, int da_version, XSQLDA in_xsqlda,
            XSQLDA out_xsqlda) throws GDSException;

    public abstract boolean native_isc_dsql_fetch(IscStmtHandle stmt_handle,
            int da_version, XSQLDA xsqlda, int fetchSize) throws GDSException;

    public abstract void native_isc_dsql_free_statement(
            IscStmtHandle stmt_handle, int option) throws GDSException;

    public abstract XSQLDA native_isc_dsql_prepare(IscTrHandle tr_handle,
            IscStmtHandle stmt_handle, byte[] statement, int dialect)
            throws GDSException;

    public abstract void native_isc_dsql_set_cursor_name(
            IscStmtHandle stmt_handle, String cursor_name, int type)
            throws GDSException;

    public abstract byte[] native_isc_dsql_sql_info(IscStmtHandle stmt_handle,
            byte[] items, int buffer_length) throws GDSException;

    public abstract byte[] native_isc_get_segment(IscBlobHandle blob,
            int maxread) throws GDSException;

    public abstract void native_isc_open_blob2(IscDbHandle db, IscTrHandle tr,
            IscBlobHandle blob, byte[] dpbBytes);

    public abstract void native_isc_prepare_transaction(IscTrHandle tr_handle)
            throws GDSException;

    public abstract void native_isc_prepare_transaction2(IscTrHandle tr_handle,
            byte[] bytes) throws GDSException;

    public abstract void native_isc_put_segment(IscBlobHandle blob_handle,
            byte[] buffer) throws GDSException;

    public abstract void native_isc_rollback_retaining(IscTrHandle tr_handle)
            throws GDSException;

    public abstract void native_isc_rollback_transaction(IscTrHandle tr_handle)
            throws GDSException;

    public abstract void native_isc_seek_blob(isc_blob_handle_impl handle,
            int position, int mode) throws GDSException;

    // Services API abstract methods
    public abstract void native_isc_service_attach(String service,
            IscSvcHandle serviceHandle, byte[] serviceParameterBuffer)
            throws GDSException;

    public abstract void native_isc_service_detach(IscSvcHandle serviceHandle)
            throws GDSException;

    public abstract void native_isc_service_query(IscSvcHandle serviceHandle,
            byte[] sendServiceParameterBuffer,
            byte[] requestServiceParameterBuffer, byte[] resultBuffer)
            throws GDSException;

    public abstract void native_isc_service_start(IscSvcHandle serviceHandle,
            byte[] serviceParameterBuffer) throws GDSException;

    public abstract void native_isc_start_transaction(IscTrHandle tr_handle,
            IscDbHandle db_handle,
            // Set tpb) throws GDSException;
            byte[] tpb) throws GDSException;
    
    public abstract void native_isc_reconnect_transaction(IscDbHandle dbHandle,
            IscTrHandle trHandle, byte[] txId) throws GDSException;
    
    public abstract byte[] native_isc_transaction_info(IscTrHandle tr_handle,
            byte[] items, int bufferSize) throws GDSException;

    public abstract int native_isc_que_events(IscDbHandle db_handle,
            EventHandleImp eventHandle, EventHandler handler) 
            throws GDSException;

    public abstract long native_isc_event_block(EventHandleImp eventHandle,
            String eventNames) throws GDSException;

    public abstract void native_isc_event_counts(EventHandleImp eventHandle)
            throws GDSException;

    public abstract void native_isc_cancel_events(IscDbHandle db_handle,
            EventHandleImp eventHandle) throws GDSException;
    
    public abstract void native_fb_cancel_operation(IscDbHandle dbHanle, 
            int kind) throws GDSException;


    public TransactionParameterBuffer newTransactionParameterBuffer() {
        return new TransactionParameterBufferImpl();
    }

    /**
     * Parse database info returned after attach. This method assumes that it is
     * not truncated.
     * 
     * @param info
     *            information returned by isc_database_info call
     * @param handle
     *            isc_db_handle to set connection parameters
     * @throws GDSException
     *             if something went wrong :))
     */
    private void parseAttachDatabaseInfo(byte[] info, IscDbHandle handle)
            throws GDSException {
        boolean debug = log != null && log.isDebugEnabled();
        if (debug)
            log.debug("parseDatabaseInfo: first 2 bytes are "
                    + iscVaxInteger(info, 0, 2) + " or: " + info[0] + ", "
                    + info[1]);
        int value = 0;
        int len = 0;
        int i = 0;
        isc_db_handle_impl db = (isc_db_handle_impl) handle;
        while (info[i] != ISCConstants.isc_info_end) {
            switch (info[i++]) {
                case ISCConstants.isc_info_db_sql_dialect:
                    len = iscVaxInteger(info, i, 2);
                    i += 2;
                    value = iscVaxInteger(info, i, len);
                    i += len;
                    db.setDialect(value);
                    if (debug) log.debug("isc_info_db_sql_dialect:" + value);
                    break;
                case ISCConstants.isc_info_ods_version:
                    len = iscVaxInteger(info, i, 2);
                    i += 2;
                    value = iscVaxInteger(info, i, len);
                    i += len;
                    db.setODSMajorVersion(value);
                    if (debug) log.debug("isc_info_ods_version:" + value);
                    break;
                case ISCConstants.isc_info_ods_minor_version:
                    len = iscVaxInteger(info, i, 2);
                    i += 2;
                    value = iscVaxInteger(info, i, len);
                    i += len;
                    db.setODSMinorVersion(value);
                    if (debug)
                        log.debug("isc_info_ods_minor_version:" + value);
                    break;
                case ISCConstants.isc_info_firebird_version:
                    len = iscVaxInteger(info, i, 2);
                    i += 2;
                    byte[] fb_vers = new byte[len - 2];
                    System.arraycopy(info, i + 2, fb_vers, 0, len - 2);
                    i += len;
                    String fb_versS = new String(fb_vers);
                    db.setVersion(fb_versS);
                    if (debug)
                        log.debug("isc_info_firebird_version:" + fb_versS);
                    break;
                case ISCConstants.isc_info_implementation:
                    len = iscVaxInteger(info, i, 2);
                    i += 2;
                    byte[] impl = new byte[len - 2];
                    System.arraycopy(info, i + 2, impl, 0, len - 2);
                    i += len;
                    break;
                case ISCConstants.isc_info_db_class:
                    len = iscVaxInteger(info, i, 2);
                    i += 2;
                    byte[] db_class = new byte[len - 2];
                    System.arraycopy(info, i + 2, db_class, 0, len - 2);
                    i += len;
                    break;
                case ISCConstants.isc_info_base_level:
                    len = iscVaxInteger(info, i, 2);
                    i += 2;
                    byte[] base_level = new byte[len - 2];
                    System.arraycopy(info, i + 2, base_level, 0, len - 2);
                    i += len;
                    break;
                case ISCConstants.isc_info_truncated:
                    if (debug) log.debug("isc_info_truncated ");
                    return;
                default:
                    throw new GDSException(ISCConstants.isc_dsql_sqlda_err);
            }
        }
    }
    
    public void readSQLData(XSQLDA xsqlda, isc_stmt_handle_impl stmt) {
        // This only works if not (port->port_flags & PORT_symmetric)
        int numCols = xsqlda.sqld;
        byte[][] row = new byte[numCols][];
        for (int i = 0; i < numCols; i++) {

            // isc_vax_integer( xsqlda.sqlvar[i].sqldata, 0,
            // xsqlda.sqlvar[i].sqldata.length );

            row[i] = xsqlda.sqlvar[i].sqldata;
        }
        if (stmt != null) stmt.addRow(row);
    }

    protected byte[] getByteArrayForString(String statement, String encoding)
            throws UnsupportedEncodingException {
        String javaEncoding = null;
        if (encoding != null && !"NONE".equals(encoding))
            javaEncoding = EncodingFactory.getJavaEncoding(encoding);

        final byte[] stringBytes;
        if (javaEncoding != null)
            stringBytes = statement.getBytes(javaEncoding);
        else
            stringBytes = statement.getBytes();

        return getZeroTerminatedArray(stringBytes);
    }

    protected byte[] getZeroTerminatedArray(byte[] stringBytes) {
        final byte[] zeroTermBytes = new byte[stringBytes.length + 1];
        System.arraycopy(stringBytes, 0, zeroTermBytes, 0, stringBytes.length);
        zeroTermBytes[stringBytes.length] = 0;

        return zeroTermBytes;
    }

    public void getSqlCounts(IscStmtHandle stmt_handle) throws GDSException {
        isc_stmt_handle_impl stmt = (isc_stmt_handle_impl) stmt_handle;
        byte[] buffer = iscDsqlSqlInfo(stmt, /* stmtInfo.length, */stmtInfo, INFO_SIZE);

        stmt.setInsertCount(0);
		stmt.setUpdateCount(0);
		stmt.setDeleteCount(0);
		stmt.setSelectCount(0);

        int pos = 0;
        int length;
        int type;
        while ((type = buffer[pos++]) != ISCConstants.isc_info_end) {
            length = iscVaxInteger2(buffer, pos);
            pos += 2;
            switch (type) {
                case ISCConstants.isc_info_sql_records:
                    int l;
                    int t;
                    while ((t = buffer[pos++]) != ISCConstants.isc_info_end) {
                        l = iscVaxInteger2(buffer, pos);
                        pos += 2;
                        switch (t) {
                            case ISCConstants.isc_info_req_insert_count:
                                stmt.setInsertCount(iscVaxInteger(buffer, pos,
                                        l));
                                break;
                            case ISCConstants.isc_info_req_update_count:
                                stmt.setUpdateCount(iscVaxInteger(buffer, pos,
                                        l));
                                break;
                            case ISCConstants.isc_info_req_delete_count:
                                stmt.setDeleteCount(iscVaxInteger(buffer, pos,
                                        l));
                                break;
                            case ISCConstants.isc_info_req_select_count:
                                stmt.setSelectCount(iscVaxInteger(buffer, pos,
                                        l));
                                break;
                            default:
                                break;
                        }
                        pos += l;
                    }
                    break;
                case ISCConstants.isc_info_sql_stmt_type:
                    stmt.setStatementType(iscVaxInteger(buffer, pos, length));
                    pos += length;
                    break;
                default:
                    pos += length;
                    break;
            }
        }
    }

    public int iscQueueEvents(IscDbHandle dbHandle, 
            EventHandle eventHandle, EventHandler eventHandler) 
            throws GDSException {

        validateHandle(dbHandle);

        EventHandleImp eventHandleImp = (EventHandleImp)eventHandle;
        if (!eventHandleImp.isValid()){
            throw new IllegalStateException(
                    "Can't queue events on an invalid EventHandle");
        }
        if (eventHandleImp.isCancelled()){
            throw new IllegalStateException(
                    "Can't queue events on a cancelled EventHandle");
        }
        synchronized (dbHandle) {
            return native_isc_que_events(
                    dbHandle, eventHandleImp, eventHandler);
        }
    }

    public void iscEventBlock(EventHandle eventHandle) 
            throws GDSException {
        
        EventHandleImp eventHandleImp = (EventHandleImp)eventHandle;
        native_isc_event_block(
                eventHandleImp, eventHandle.getEventName());
    }

    public void iscEventCounts(EventHandle eventHandle)
            throws GDSException {

        EventHandleImp eventHandleImp = (EventHandleImp)eventHandle;
        if (!eventHandleImp.isValid()){
            throw new IllegalStateException(
                    "Can't get counts on an invalid EventHandle");
        }
        native_isc_event_counts(eventHandleImp);
    }


    public void iscCancelEvents(IscDbHandle dbHandle, EventHandle eventHandle)
            throws GDSException {

        validateHandle(dbHandle);
        EventHandleImp eventHandleImp = (EventHandleImp)eventHandle;
        if (!eventHandleImp.isValid()){
            throw new IllegalStateException(
                    "Can't cancel an invalid EventHandle");
        }
        if (eventHandleImp.isCancelled()){
            throw new IllegalStateException(
                    "Can't cancel a previously cancelled EventHandle");
        }
        eventHandleImp.cancel();
        synchronized (dbHandle){
            native_isc_cancel_events(dbHandle, eventHandleImp);
        }
    }

    public EventHandle createEventHandle(String eventName){
        return new EventHandleImp(eventName);
    }
    
    public void fbCancelOperation(IscDbHandle dbHandle, int kind)
            throws GDSException {
        validateHandle(dbHandle);

        native_fb_cancel_operation(dbHandle, kind);
    }

    /**
     * Validates if the database handle is valid for use and casts it to {@link isc_db_handle_impl}
     *
     * @param dbHandle Database handle
     * @return <tt>dbHandle</tt> cast to <tt>isc_db_handle_impl</tt>
     * @throws GDSException If the <tt>dbHandle</tt> is <tt>null</tt> or invalid
     */
    protected final isc_db_handle_impl validateHandle(final IscDbHandle dbHandle) throws GDSException {
        if (dbHandle == null || !dbHandle.isValid()) {
            throw new GDSException(ISCConstants.isc_bad_db_handle);
        }
        return (isc_db_handle_impl) dbHandle;
    }

    /**
     * Validates if the blob handle is valid for use and casts it to {@link isc_blob_handle_impl}
     *
     * @param blobHandle Blob handle
     * @return <tt>blobHandle</tt> cast to <tt>isc_blob_handle_impl</tt>
     * @throws GDSException If the <tt>blobHandle</tt> is <tt>null</tt>
     */
    protected final isc_blob_handle_impl validateHandle(final IscBlobHandle blobHandle) throws GDSException {
        if (blobHandle == null) {
            throw new GDSException(ISCConstants.isc_bad_segstr_handle);
        }
        return (isc_blob_handle_impl) blobHandle;
    }

    /**
     * Validates if the transaction handle is valid for use and casts it to {@link isc_tr_handle_impl}
     *
     * @param transactionHandle Transaction handle
     * @return <tt>transactionHandle</tt> cast to <tt>isc_tr_handle_impl</tt>
     * @throws GDSException If the <tt>transactionHandle</tt> is <tt>null</tt>
     */
    protected final isc_tr_handle_impl validateHandle(final IscTrHandle transactionHandle) throws GDSException {
        if (transactionHandle == null) {
            throw new GDSException(ISCConstants.isc_bad_trans_handle);
        }
        return (isc_tr_handle_impl) transactionHandle;
    }

    /**
     * Validates if the statement handle is valid for use and casts it to {@link isc_stmt_handle_impl}
     *
     * @param statementHandle Statement handle
     * @return <tt>statementHandle</tt> cast to <tt>isc_stmt_handle_impl</tt>
     * @throws GDSException If the <tt>statementHandle</tt> is <tt>null</tt>
     */
    protected final isc_stmt_handle_impl validateHandle(final IscStmtHandle statementHandle) throws GDSException {
        // Not checking statementHandle.isValid() as that simply checks database validity which is done everywhere this is called as well
        if (statementHandle == null) {
            throw new GDSException(ISCConstants.isc_bad_stmt_handle);
        }
        return (isc_stmt_handle_impl) statementHandle;
    }

    /**
     * Validates if the service handle is valid for use and casts it to {@link isc_svc_handle_impl}
     *
     * @param svcHandle Service handle
     * @return <tt>svcHandle</tt> cast to <tt>isc_svc_handle_impl</tt>
     * @throws GDSException If the <tt>svcHandle</tt> is <tt>null</tt> or invalid
     */
    protected final isc_svc_handle_impl validateHandle(final IscSvcHandle svcHandle) throws GDSException {
        if (svcHandle == null || svcHandle.isNotValid()) {
            throw new GDSException(ISCConstants.isc_bad_svc_handle);
        }
        return (isc_svc_handle_impl) svcHandle;
    }
}