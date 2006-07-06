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

package org.firebirdsql.jca;


// imports --------------------------------------







import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.firebirdsql.gds.Clumplet;
import org.firebirdsql.gds.GDS;
import org.firebirdsql.gds.GDSException;
import org.firebirdsql.gds.GDSFactory;
import org.firebirdsql.gds.isc_db_handle;
import org.firebirdsql.gds.isc_tr_handle;
import org.firebirdsql.jdbc.FBConnectionHelper;
import org.firebirdsql.jdbc.FBDataSource;
import org.firebirdsql.jdbc.FBStatement;
import org.firebirdsql.logging.Logger;
import org.firebirdsql.logging.LoggerFactory;

/**
 * FBManagedConnectionFactory implements the jca ManagedConnectionFactory 
 * interface and also many of the internal functions of ManagedConnection.
 * This nonstandard behavior is required due to firebird requiring
 * all work done in a transaction to be done over one connection.
 * To support xa semantics, the correct db handle must be located whenever 
 * a ManagedConnection is associated with an xid.
 *
 * WARNING: this adapter will probably not work properly in an environment
 * where ManagedConnectionFactory is serialized and deserialized, and the 
 * deserialized copy is expected to function as anything other than a key.
 *
 * @see <related>
 * @author <a href="mailto:d_jencks@users.sourceforge.net">David Jencks</a>
 * @version $ $
 *
 * @todo add support for separate specification of host/port/filename.
 */


public class FBManagedConnectionFactory implements  ManagedConnectionFactory {

   private final Logger log = LoggerFactory.getLogger(getClass(),true);

    final GDS gds = GDSFactory.newGDS();

    //should add handling for host/port/file separately.
    private String dbAlias;

    private final Map criToFreeDbHandlesMap = new HashMap();

    //Maps supplied XID to internal transaction handle.
    //a concurrent reader map would be better
    private final Map xidMap = Collections.synchronizedMap(new HashMap());

    //Maps transaction handle to list of statements with resultsets.
    private final Map TransactionStatementMap = new HashMap();

    private FBConnectionRequestInfo defaultCri;

    private final FBTpb tpb = new FBTpb();

    private final static int MAX_BLOB_BUFFER_LENGTH = 1024 * 32 - 1;
    private final static int MIN_BLOB_BUFFER_LENGTH = 1024;


    //must be less than 1024 * 32: 1-24 * 32 -  is ok.
    private int blobBufferLength = 1024 * 16;

    private volatile int hashCode = 0;

    public FBManagedConnectionFactory() {
        defaultCri = FBConnectionHelper.getDefaultCri();
    }  //Default constructor.

    //rar properties


    public void setDatabase(String database) throws ResourceException {
        if (this.dbAlias!=null && !database.equals(this.dbAlias))
            throw new ResourceException("can't change database!");
        hashCode = 0;
        this.dbAlias = database;
    }

    public String getDatabase() {
        return dbAlias;
    }

    public void setConnectionRequestInfo(FBConnectionRequestInfo cri) {
        hashCode = 0;
        this.defaultCri = new FBConnectionRequestInfo(cri);
    }


    public FBConnectionRequestInfo getDefaultConnectionRequestInfo() {
        return new FBConnectionRequestInfo(defaultCri);
    }

    public void setUserName(String userName)
    {
        hashCode = 0;
        defaultCri.setUser(userName);
    }

    public String getUserName()
    {
        return defaultCri.getUser();
    }

    public void setPassword(String password)
    {
        hashCode = 0;
        defaultCri.setPassword(password);
    }

    public String getPassword()
    {
        return defaultCri.getPassword();
    }

    public void setTpb(FBTpb tpb)
    {
        hashCode = 0;
        this.tpb.setTpb(tpb);
    }


    public FBTpb getTpb() {
        return new FBTpb(tpb);
    }

    public void setTransactionIsolation(Integer level) throws ResourceException
    {
        hashCode = 0;
        if (level == null) 
        {
            throw new ResourceException("You must supply a isolation level");
        } // end of if ()
        else
        {
            tpb.setTransactionIsolation(level.intValue());
        } // end of else
    }

    public Integer getTransactionIsolation() throws ResourceException
    {
        return new Integer(tpb.getTransactionIsolation());
    }

    public void setTransactionIsolationName(String level) throws ResourceException
    {
        hashCode = 0;
        tpb.setTransactionIsolationName(level);
    }

    public String getTransactionIsolationName() throws ResourceException
    {
        return tpb.getTransactionIsolationName();
    }
    
    public void setEncoding(String encoding) {
        hashCode = 0;
        defaultCri.setProperty(GDS.isc_dpb_lc_ctype, encoding);
    }
    
    public String getEncoding() {
        String result = defaultCri.getStringProperty(GDS.isc_dpb_lc_ctype);
        if (result == null)
            result = "NONE";
        return result;
    }

    /**
     * Get the BlobBufferLength value.
     * @return the BlobBufferLength value.
     */
    public int getBlobBufferLength()
    {
        return blobBufferLength;
    }

    /**
     * Set the BlobBufferLength value.
     * @param newBlobBufferLength The new BlobBufferLength value.
     */
    public void setBlobBufferLength(final int blobBufferLength)
    {
        hashCode = 0;
        if (blobBufferLength > MAX_BLOB_BUFFER_LENGTH) 
        {
            this.blobBufferLength = MAX_BLOB_BUFFER_LENGTH;
            if (log!=null) log.warn("Supplied blob buffer length greater than maximum of " + MAX_BLOB_BUFFER_LENGTH);
        } // end of if ()
        else if (blobBufferLength < MIN_BLOB_BUFFER_LENGTH ) 
        {
            this.blobBufferLength = MIN_BLOB_BUFFER_LENGTH;
            if (log!=null) log.warn("Supplied blob buffer length less than minimum of " + MIN_BLOB_BUFFER_LENGTH);
        } // end of if ()
        else
        {
            this.blobBufferLength = blobBufferLength;
        } // end of else
    }


    public int hashCode()
    {
        if (hashCode != 0) 
        {
            return hashCode;
        } // end of if ()
        int result = 17;
        result = 37 * result + ((dbAlias == null)? 0: dbAlias.hashCode());
        result = 37 * result + defaultCri.hashCode();
        result = 37 * result + tpb.hashCode();
        result = 37 * result + blobBufferLength;
        hashCode = result;
        return hashCode;
    }
    
    public boolean equals(Object other)
    {
        if (other == this) 
        {
            return true;
        } // end of if ()
        if (!(other instanceof FBManagedConnectionFactory)) 
        {
            return false;
        } // end of if ()
        FBManagedConnectionFactory mcf = (FBManagedConnectionFactory)other;
        return
            (dbAlias == null ? mcf.dbAlias == null : dbAlias.equals(mcf.dbAlias))
            && (defaultCri.equals(mcf.defaultCri))
            && (tpb.equals(mcf.tpb))
            && (blobBufferLength == blobBufferLength);
    }

    /**
     * The <code>createConnectionFactory</code> method creates a DataSource
     * using the supplied ConnectionManager..
     *
     * @param cxManager a <code>ConnectionManager</code> value
     * @return a <code>java.lang.Object</code> value
     * @exception ResourceException if an error occurs
     */
    public java.lang.Object createConnectionFactory(ConnectionManager cxManager) throws ResourceException {
        return new FBDataSource(this, cxManager);
    }


    /**
     * The <code>createConnectionFactory</code> method creates a DataSource
     * with a default stand alone ConnectionManager.  Ours can implement pooling.
     *
     * @return a <code>java.lang.Object</code> value
     * @exception ResourceException if an error occurs
     */
    public java.lang.Object createConnectionFactory() throws ResourceException {
        return new FBDataSource(this, new FBStandAloneConnectionManager());
    }



/**
     Creates a new physical connection to the underlying EIS resource manager,

     ManagedConnectionFactory uses the security information (passed as Subject) and additional
     ConnectionRequestInfo (which is specific to ResourceAdapter and opaque to application server)
     to create this new connection.
     Parameters:
         Subject - Caller's security information
         cxRequestInfo - Additional resource adapter specific connection request information
     Returns:
         ManagedConnection instance
     Throws:
         ResourceException - generic exception
         SecurityException - security related error
         ResourceAllocationException - failed to allocate system resources for connection
         request
         ResourceAdapterInternalException - resource adapter related error condition
         EISSystemException - internal error condition in EIS instance

**/
    public ManagedConnection createManagedConnection(Subject subject,
                                                     ConnectionRequestInfo cri)
        throws ResourceException 
    {
        return new FBManagedConnection(subject, cri, this);
    }




/**
     Returns a matched connection from the candidate set of connections.

     ManagedConnectionFactory uses the security info (as in Subject) and information provided
     through ConnectionRequestInfo and additional Resource Adapter specific criteria to do
     matching. Note that criteria used for matching is specific to a resource adapter and is not
     prescribed by the Connector specification.

     This method returns a ManagedConnection instance that is the best match for handling the
     connection allocation request.

     Parameters:
         connectionSet - candidate connection set
         Subject - caller's security information
         cxRequestInfo - additional resource adapter specific connection request information
     Returns:
         ManagedConnection if resource adapter finds an acceptable match otherwise null
     Throws:
         ResourceException - generic exception
         SecurityException - security related error
         ResourceAdapterInternalException - resource adapter related error condition
         NotSupportedException - if operation is not supported

**/

    public ManagedConnection matchManagedConnections(java.util.Set connectionSet,
                                                 javax.security.auth.Subject subject,
                                                 ConnectionRequestInfo cxRequestInfo)
                                          throws ResourceException {
    Iterator i = connectionSet.iterator();
    while (i.hasNext()) {
        FBManagedConnection mc = (FBManagedConnection)i.next();
            if (mc.matches(subject, (FBConnectionRequestInfo)cxRequestInfo)) {
                return mc;
            }
    }
    return null;
    }




/**
     Set the log writer for this ManagedConnectionFactory instance.

     The log writer is a character output stream to which all logging and tracing messages for this
     ManagedConnectionfactory instance will be printed.

     ApplicationServer manages the association of output stream with the
     ManagedConnectionFactory. When a ManagedConnectionFactory object is created the log
     writer is initially null, in other words, logging is disabled. Once a log writer is associated with a
     ManagedConnectionFactory, logging and tracing for ManagedConnectionFactory instance is
     enabled.

     The ManagedConnection instances created by ManagedConnectionFactory "inherits" the log
     writer, which can be overridden by ApplicationServer using ManagedConnection.setLogWriter
     to set ManagedConnection specific logging and tracing.
     Parameters:
         out - PrintWriter - an out stream for error logging and tracing
     Throws:
         ResourceException - generic exception
         ResourceAdapterInternalException - resource adapter related error condition

**/

    public void setLogWriter(PrintWriter out) throws ResourceException {
       //ignore - we're using log4j
    }



/**
     Get the log writer for this ManagedConnectionFactory instance.

     The log writer is a character output stream to which all logging and tracing messages for this
     ManagedConnectionFactory instance will be printed

     ApplicationServer manages the association of output stream with the
     ManagedConnectionFactory. When a ManagedConnectionFactory object is created the log
     writer is initially null, in other words, logging is disabled.
     Returns:
         PrintWriter
     Throws:
         ResourceException - generic exception

**/
    public PrintWriter getLogWriter() {
       return null;//we're using log4j
    }


    isc_tr_handle getTrHandleForXid(Xid xid)
    {
        return (isc_tr_handle)xidMap.get(xid);
    }


    isc_tr_handle getCurrentIscTrHandle(Xid xid, FBManagedConnection mc, int flags) 
        throws XAException 
    {
        isc_tr_handle tr = getTrHandleForXid(xid);
        if (tr == null) {
            if (flags != XAResource.TMNOFLAGS) {
                throw new XAException("Transaction flags wrong, this xid new for this rm");
            }
            //new xid for us
            isc_db_handle db = mc.getIscDBHandle();
            tr = gds.get_new_isc_tr_handle();
            try {
                gds.isc_start_transaction(tr, db, mc.getTpb());
            }
            catch (GDSException ge) {
                throw new XAException(ge.getMessage());
            }
            xidMap.put(xid, tr);
        }
        else {
            if (flags != XAResource.TMJOIN && flags != XAResource.TMRESUME) {
                throw new XAException("Transaction flags wrong, this xid already known");
            }
        }
        return tr;
    }


    isc_db_handle getDbHandle(FBConnectionRequestInfo cri) throws XAException 
    {
        try 
        {
            try 
            {
                LinkedList freeDbHandles = null;
                synchronized (criToFreeDbHandlesMap)
                {
                    freeDbHandles = (LinkedList)criToFreeDbHandlesMap.get(cri);
                }
                if (freeDbHandles != null) 
                {
                    isc_db_handle db = null;
                    synchronized (freeDbHandles) 
                    {
                        db = (isc_db_handle)freeDbHandles.removeLast();
                    }
                    return db;
                } // end of if ()
                return createDbHandle(cri);
            } 
            catch (NoSuchElementException e) 
            {
                return createDbHandle(cri);
            }
        }
        catch (GDSException ge)
        {
            if (log!=null) log.error("GDS Exception in getDbHandle", ge);
            throw new XAException(ge.getMessage());
        } // end of try-catch
    }

    private isc_db_handle createDbHandle(FBConnectionRequestInfo cri) throws GDSException
    {
        isc_db_handle db = gds.get_new_isc_db_handle();
        gds.isc_attach_database(dbAlias, db, cri.getDpb());
        return db;
    }

    void assureDbHandle(FBConnectionRequestInfo cri) throws ResourceException
    {
        try 
        {
            returnDbHandle(createDbHandle(cri), cri);
        }
        catch (GDSException ge)
        {
            if (log!=null) log.info("Could not get a db connection!", ge);
            throw new ResourceException(ge.getMessage());   
        } // end of try-catch
    }
        

    void returnDbHandle(isc_db_handle db, FBConnectionRequestInfo cri) 
    {
        if (db != null) 
        {
            LinkedList freeDbHandles = null;
            synchronized(criToFreeDbHandlesMap)
            {
                freeDbHandles = (LinkedList)criToFreeDbHandlesMap.get(cri);
                if (freeDbHandles == null) 
                {
                    freeDbHandles = new LinkedList();
                    criToFreeDbHandlesMap.put(cri, freeDbHandles);
                } // end of if ()
            }
            synchronized(freeDbHandles)
            {
                freeDbHandles.addLast(db);
            }
        }
    }

    void releaseDbHandle(isc_db_handle db, FBConnectionRequestInfo cri) 
        throws GDSException 
    {
        synchronized (db)
        {
            if (db != null && !db.hasTransactions())
            {
                gds.isc_detach_database(db);
            }
        }
    }




    void commit(Xid xid) throws XAException {
        isc_tr_handle tr = getTrHandleForXid(xid);
        forgetResultSets(tr);
        try {
            gds.isc_commit_transaction(tr);
        }
        catch (GDSException ge) {
            throw new XAException(ge.getMessage());
        }
        finally {
            xidMap.remove(xid);
        }
    }

    void prepare(Xid xid) throws XAException {
        try {
            FBXid fbxid;
            if (xid instanceof FBXid) {
                fbxid = (FBXid)xid;
            }
            else {
                fbxid = new FBXid(xid);
            }
            gds.isc_prepare_transaction2(getTrHandleForXid(xid), fbxid.toBytes());
        }
        catch (GDSException ge) {
            if (log!=null) log.warn("error in prepare", ge);
            xidMap.remove(xid);
            throw new XAException(ge.getMessage());
        }
    }

    void rollback(Xid xid) throws XAException {
        isc_tr_handle tr = getTrHandleForXid(xid);
        forgetResultSets(tr);
        try {
            gds.isc_rollback_transaction(tr);
        }
        catch (GDSException ge) {
            throw new XAException(ge.getMessage());
        }
        finally {
            xidMap.remove(xid);
        }
    }


    void registerStatementWithTransaction(isc_tr_handle tr, FBStatement stmt) {
        ArrayList stmts = null;
        synchronized (tr) {
            stmts = (ArrayList)TransactionStatementMap.get(tr);
            if (stmts == null) {
                stmts = new ArrayList();
                TransactionStatementMap.put(tr, stmts);
            }
        }
        stmts.add(stmt);
    }

    private void forgetResultSets(isc_tr_handle tr) {
        //shouldn't need synchronization, only called by rollback and commit- then we're done
        //transaction/thread should also help.
        ArrayList stmts = (ArrayList)TransactionStatementMap.get(tr);
        if (stmts != null) {
            Iterator i = stmts.iterator();
            while (i.hasNext()) {
                ((FBStatement)i.next()).forgetResultSet();
            }
            stmts.clear();
        }
    }



}




