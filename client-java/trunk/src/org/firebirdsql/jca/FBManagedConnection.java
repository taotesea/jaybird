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

import javax.resource.ResourceException;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;

import java.util.ArrayList;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import javax.security.auth.Subject;

import org.firebirdsql.gds.isc_db_handle;
import org.firebirdsql.gds.isc_tr_handle;
import org.firebirdsql.gds.GDS;

/**
 *
 *   @see <related>
 *   @author David Jencks (davidjencks@earthlink.net)
 *   @version $ $
 */



/**This class implements the ManagedConnection interface.

/**
 * <p>
 */

public class FBManagedConnection implements ManagedConnection, XAResource {
    
    private FBManagedConnectionFactory mcf;
    
    private ArrayList connectionEventListeners = new ArrayList();
    
    private int timeout = 0;
    
    private Subject s;
    
    
    private isc_tr_handle currentTr;
    
    private isc_db_handle currentDbHandle;
    
    FBManagedConnection(Subject s, FBManagedConnectionFactory mcf) {
        this.mcf = mcf;
        this.s = s;
    }
    
    
    
    //javax.resource.spi.ManagedConnection implementation
    
    /**
     Returns an javax.resource.spi.LocalTransaction instance. The LocalTransaction interface
     is used by the container to manage local transactions for a RM instance.
     Returns:
         LocalTransaction instance
     Throws:
         ResourceException - generic exception if operation fails
         NotSupportedException - if the operation is not supported
         ResourceAdapterInternalException - resource adapter internal error condition
    
    
    
    **/

    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new ResourceException("not yet implemented");
    }



    /**
     Gets the metadata information for this connection's underlying EIS resource manager instance.
     The ManagedConnectionMetaData interface provides information about the underlying EIS
     instance associated with the ManagedConenction instance.
     Returns:
         ManagedConnectionMetaData instance
     Throws:
         ResourceException - generic exception if operation fails
         NotSupportedException - if the operation is not supported
    **/
    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        throw new ResourceException("not yet implemented");
    }
    
    /**
     Sets the log writer for this ManagedConnection instance. 

     The log writer is a character output stream to which all logging and tracing messages for this
     ManagedConnection instance will be printed. Application Server manages the association of
     output stream with the ManagedConnection instance based on the connection pooling
     requirements.

     When a ManagedConnection object is initially created, the default log writer associated with this
     instance is obtained from the ManagedConnectionFactory. An application server can set a log
     writer specific to this ManagedConnection to log/trace this instance using setLogWriter method.

     Parameters:
         out - Character Output stream to be associated
     Throws:
         ResourceException - generic exception if operation fails
         ResourceAdapterInternalException - resource adapter related error condition
    **/
    public void setLogWriter(java.io.PrintWriter out) throws ResourceException {
        throw new ResourceException("not yet implemented");
    }
    
    
    /**
     Gets the log writer for this ManagedConnection instance. 

     The log writer is a character output stream to which all logging and tracing messages for this
     ManagedConnection instance will be printed. ConnectionManager manages the association of
     output stream with the ManagedConnection instance based on the connection pooling
     requirements.

     The Log writer associated with a ManagedConnection instance can be one set as default from the
     ManagedConnectionFactory (that created this connection) or one set specifically for this
     instance by the application server.

     Returns:
         Character ourput stream associated with this Managed- Connection instance
     Throws:
         ResourceException - generic exception if operation fails
    **/

    public java.io.PrintWriter getLogWriter() throws ResourceException {
        throw new ResourceException("not yet implemented");
    }
    
  /**<P> Add an event listener.
   */
    public void addConnectionEventListener(ConnectionEventListener listener) {
        connectionEventListeners.add(listener);
    }



  /**<P> Remove an event listener.
   */
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        connectionEventListeners.remove(listener);
    }
    
  /**Used by the container to change the association of an application-level connection handle with a
     ManagedConneciton instance. The container should find the right ManagedConnection instance
     and call the associateConnection method. 

     The resource adapter is required to implement the associateConnection method. The method
     implementation for a ManagedConnection should dissociate the connection handle (passed as a
     parameter) from its currently associated ManagedConnection and associate the new connection
     handle with itself.
     Parameters:
         connection - Application-level connection handle
     Throws:
         ResourceException - Failed to associate the connection handle with this
         ManagedConnection instance
         IllegalStateException - Illegal state for invoking this method
         ResourceAdapterInternalException - Resource adapter internal error condition
*/
    public void associateConnection(java.lang.Object connection) throws ResourceException {
        throw new ResourceException("not yet implemented");
    }
/**


     Application server calls this method to force any cleanup on the ManagedConnection instance. 

     The method ManagedConnection.cleanup initiates a cleanup of the any client-specific state as
     maintained by a ManagedConnection instance. The cleanup should invalidate all connection
     handles that had been created using this ManagedConnection instance. Any attempt by an
     application component to use the connection handle after cleanup of the underlying
     ManagedConnection should result in an exception. 

     The cleanup of ManagedConnection is always driven by an application server. An application
     server should not invoke ManagedConnection.cleanup when there is an uncompleted transaction
     (associated with a ManagedConnection instance) in progress. 

     The invocation of ManagedConnection.cleanup method on an already cleaned-up connection
     should not throw an exception. 

     The cleanup of ManagedConnection instance resets its client specific state and prepares the
     connection to be put back in to a connection pool. The cleanup method should not cause resource
     adapter to close the physical pipe and reclaim system resources associated with the physical
     connection.
     Throws:
         ResourceException - generic exception if operation fails
         ResourceAdapterInternalException - resource adapter internal error condition
         IllegalStateException - Illegal state for calling connection cleanup. Example - if a
         localtransaction is in progress that doesn't allow connection cleanup
*/    
    public void cleanup() throws ResourceException {
        throw new ResourceException("not yet implemented");
    }
    
/**


     Creates a new connection handle for the underlying physical connection represented by the
     ManagedConnection instance. This connection handle is used by the application code to refer to
     the underlying physical connection. A connection handle is tied to its ManagedConnection
     instance in a resource adapter implementation specific way.

     The ManagedConnection uses the Subject and additional ConnectionRequest Info (which is
     specific to resource adapter and opaque to application server) to set the state of the physical
     connection.

     Parameters:
         Subject - security context as JAAS subject
         cxRequestInfo - ConnectionRequestInfo instance
     Returns:
         generic Object instance representing the connection handle. For CCI, the connection handle
         created by a ManagedConnection instance is of the type javax.resource.cci.Connection.
     Throws:
         ResourceException - generic exception if operation fails
         ResourceAdapterInternalException - resource adapter internal error condition
         SecurityException - security related error condition
         CommException - failed communication with EIS instance
         EISSystemException - internal error condition in EIS instance - used if EIS instance is
         involved in setting state of ManagedConnection
**/    
    public java.lang.Object getConnection(Subject subject, ConnectionRequestInfo cxRequestInfo)
        throws ResourceException {
        throw new ResourceException("not yet implemented");
    }
    
 
/**


     Destroys the physical connection to the underlying resource manager. 

     To manage the size of the connection pool, an application server can explictly call
     ManagedConnection.destroy to destroy a physical connection. A resource adapter should destroy
     all allocated system resources for this ManagedConnection instance when the method destroy is
     called.
     Throws:
         ResourceException - generic exception if operation failed
         IllegalStateException - illegal state for destroying connection
**/   
    public void destroy() throws ResourceException {
        throw new ResourceException("not yet implemented");
    }
             
             
  /**<P>In both javax.sql.XAConnection and javax.resource.spi.MangagedConnection
   * <P>Return an XA resource to the caller.
   *
   * @return the XAResource
   * @exception SQLException if a database-access error occurs
   */
    public javax.transaction.xa.XAResource getXAResource() throws ResourceException {
        return this;
    }
    
    //--------------------------------------------------------------
    //XAResource implementation
    //--------------------------------------------------------------
    

    /**
     * Commits a transaction.
     * @throws XAException
     *     Occurs when the state was not correct (end never called), the
     *     transaction ID is wrong, the connection was set to Auto-Commit,
     *     or the commit on the underlying connection fails.  The error code
     *     differs depending on the exact situation.
     */
    public void commit(Xid id, boolean twoPhase) throws XAException {
        if (mcf.lookupXid(id) == null) {
            throw new XAException("commit called with unknown transaction");
        }
        if (mcf.lookupXid(id) == currentTr) {
            throw new XAException("commit called with current xid");
        }
        mcf.commit(id);
    }

    /**
     * Dissociates a resource from a global transaction.
     * @throws XAException
     *     Occurs when the state was not correct (end called twice), or the
     *     transaction ID is wrong.
     */
     //what do we do with flags?????
    public void end(Xid id, int flags) throws javax.transaction.xa.XAException {
        if (currentTr == null) {
            throw new XAException("end called with no transaction associated");
        }
        if (mcf.lookupXid(id) != currentTr) {
            throw new XAException("end called with wrong xid");
        }
        currentTr = null;
    }

    /**
     * Indicates that no further action will be taken on behalf of this
     * transaction (after a heuristic failure).  It is assumed this will be
     * called after a failed commit or rollback.
     * @throws XAException
     *     Occurs when the state was not correct (end never called), or the
     *     transaction ID is wrong.
     */
    public void forget(Xid id) throws javax.transaction.xa.XAException {
        if (mcf.lookupXid(id) == null) {
            throw new XAException("forget called with unknown transaction");
        }
        if (mcf.lookupXid(id) == currentTr) {
            throw new XAException("forget called with current xid");
        }
        mcf.forgetXid(id);
    }

    /**
     * Gets the transaction timeout.
     */
    public int getTransactionTimeout() throws javax.transaction.xa.XAException {
        return timeout;
    }

    public boolean isSameRM(XAResource res) throws javax.transaction.xa.XAException {
        return (res instanceof FBManagedConnection) 
            && (mcf == ((FBManagedConnection)res).mcf); 
    }

    /**
     * Prepares a transaction to commit.  
     * @throws XAException
     *     Occurs when the state was not correct (end never called), the
     *     transaction ID is wrong, or the connection was set to Auto-Commit.
     */
    public int prepare(Xid id) throws javax.transaction.xa.XAException {
        if (mcf.lookupXid(id) == null) {
            throw new XAException("prepare called with unknown transaction");
        }
        if (mcf.lookupXid(id) == currentTr) {
            throw new XAException("prepare called with current xid");
        }
        mcf.prepare(id);
        return XA_OK;
    }

    public Xid[] recover(int flag) throws javax.transaction.xa.XAException {
/*        if(fbmc.getCurrentXid() == null)
            return new Xid[0];
        else
            return new Xid[]{fbmc.getCurrentXid()};*/
         return null;
    }

    /**
     * Rolls back the work, assuming it was done on behalf of the specified
     * transaction.
     * @throws XAException
     *     Occurs when the state was not correct (end never called), the
     *     transaction ID is wrong, the connection was set to Auto-Commit,
     *     or the rollback on the underlying connection fails.  The error code
     *     differs depending on the exact situation.
     */
    public void rollback(Xid id) throws javax.transaction.xa.XAException {
        if (mcf.lookupXid(id) == null) {
            throw new XAException("rollback called with unknown transaction");
        }
        if (mcf.lookupXid(id) == currentTr) {
            throw new XAException("rollback called with current xid");
        }
        mcf.rollback(id);
    }

    /**
     * Sets the transaction timeout.  This is saved, but the value is not used
     * by the current implementation.
     */
    public boolean setTransactionTimeout(int timeout) throws javax.transaction.xa.XAException {
        this.timeout = timeout;
        return true;
    }

    /**
     * Associates a JDBC connection with a global transaction.  We assume that
     * end will be called followed by prepare, commit, or rollback.
     * If start is called after end but before commit or rollback, there is no
     * way to distinguish work done by different transactions on the same
     * connection).  If start is called more than once before
     * end, either it's a duplicate transaction ID or illegal transaction ID
     * (since you can't have two transactions associated with one DB
     * connection).
     * @throws XAException
     *     Occurs when the state was not correct (start called twice), the
     *     transaction ID is wrong, or the instance has already been closed.
     */
    public void start(Xid id, int flags) throws XAException {
        if (currentTr != null) {
            throw new XAException("start called with transaction associated");
        }
        findIscTrHandle(id, flags);
    }
    

    //--------------------------------------------------------------------
    //package visibility
    //--------------------------------------------------------------------
    
    void findIscTrHandle(Xid xid, int flags) throws XAException {
        currentTr = mcf.getCurrentIscTrHandle(xid, this, flags);
        if (currentTr.getDbHandle() != currentDbHandle) {
            mcf.returnDbHandle(currentDbHandle);
            currentDbHandle = currentTr.getDbHandle();
        }
    }
    //temporarily public for testing
    public isc_db_handle getIscDBHandle() throws XAException {
        if (currentDbHandle == null) {
            currentDbHandle = mcf.getDbHandle();
        }
        return currentDbHandle;
    }
    


    
}
