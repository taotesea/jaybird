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
package org.firebirdsql.pool;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Hashtable;

import javax.naming.*;
import javax.naming.spi.ObjectFactory;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;

import org.firebirdsql.jdbc.FBConnectionHelper;
import org.firebirdsql.jdbc.FBSQLException;

/**
 * Base class for connection pool implementations. Main feature of this class is
 * that it implements {@link org.firebirdsql.pool.ConnectionPoolConfiguration}
 * interface and releives developers from creating getters and setters for 
 * pool configuration parameters. Additionally this class provides basic 
 * functionality for JNDI-enabled connection pools.
 * 
 * No other functionality is available.
 * 
 * @author <a href="mailto:rrokytskyy@users.sourceforge.net">Roman Rokytskyy</a>
 */
public abstract class BasicAbstractConnectionPool 
    extends AbstractConnectionPool 
    implements ConnectionPoolConfiguration, ConnectionPoolDataSource,
    Serializable, Referenceable, ObjectFactory
{

    /*
     * Following fields contain information about the pool characteristics.
     */
    private int minConnections = FBPoolingDefaults.DEFAULT_MIN_SIZE;
    private int maxConnections = FBPoolingDefaults.DEFAULT_MAX_SIZE;

    private int blockingTimeout = FBPoolingDefaults.DEFAULT_BLOCKING_TIMEOUT;
    private int retryInterval = FBPoolingDefaults.DEFAULT_RETRY_INTERVAL;
    private int idleTimeout = FBPoolingDefaults.DEFAULT_IDLE_TIMEOUT;

    private int pingInterval = FBPoolingDefaults.DEFAULT_PING_INTERVAL;
    private String pingStatement;

    private boolean pooling = true;
    private boolean statementPooling = true;
    private int transactionIsolation = FBPoolingDefaults.DEFAULT_ISOLATION;
    
    private Reference reference;
    
    /**
     * Create instance of this class. Default constructor introduced to make
     * it available to subclasses.
     */
    protected BasicAbstractConnectionPool() {
        super();
    }

    public abstract int getLoginTimeout() throws SQLException;
    public abstract void setLoginTimeout(int seconds) throws SQLException;

    public abstract PrintWriter getLogWriter() throws SQLException;
    public abstract void setLogWriter(PrintWriter printWriter) throws SQLException;
    
    public abstract PooledConnection getPooledConnection() throws SQLException;
    public abstract PooledConnection getPooledConnection(
        String user, String password) throws SQLException;
    
    public ConnectionPoolConfiguration getConfiguration() {
        return this;
    }
    
    public int getBlockingTimeout() {
        return blockingTimeout;
    }

    public void setBlockingTimeout(int blockingTimeout) {
        this.blockingTimeout = blockingTimeout;
    }

    public int getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(int idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public int getMinConnections() {
        return minConnections;
    }

    public void setMinConnections(int minConnections) {
        this.minConnections = minConnections;
    }

    public int getPingInterval() {
        return pingInterval;
    }

    public void setPingInterval(int pingInterval) {
        this.pingInterval = pingInterval;
    }

    public String getPingStatement() {
        return pingStatement;
    }

    public void setPingStatement(String pingStatement) {
        this.pingStatement = pingStatement;
    }

    public int getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(int retryInterval) {
        this.retryInterval = retryInterval;
    }

    public boolean isPingable() {
        return getPingInterval() > 0 && getPingStatement() != null;
    }

    public boolean isPooling() {
        return pooling;
    }

    public void setPooling(boolean pooling) {
        this.pooling = pooling;
    }

    public boolean isStatementPooling() {
        return statementPooling;
    }

    public void setStatementPooling(boolean statementPooling) {
        this.statementPooling = statementPooling;
    }
    
    public int getTransactionIsolationLevel() {
        return transactionIsolation;
    }
    
    public void setTransactionIsolationLevel(int transactionIsolation) {
        this.transactionIsolation = transactionIsolation;
    }
    
    public String getIsolation() {
        switch(getTransactionIsolationLevel()) {
        
            case Connection.TRANSACTION_READ_COMMITTED :
                return FBConnectionHelper.TRANSACTION_READ_COMMITTED;
            
            case Connection.TRANSACTION_REPEATABLE_READ :
                return FBConnectionHelper.TRANSACTION_REPEATABLE_READ;
            
            case Connection.TRANSACTION_SERIALIZABLE :
                return FBConnectionHelper.TRANSACTION_SERIALIZABLE;
            
            default :
                throw new IllegalStateException("Unknown transaction isolation level");
        }
    }
    
    public void setIsolation(String isolation) throws SQLException {
        if (FBConnectionHelper.TRANSACTION_READ_COMMITTED.equalsIgnoreCase(isolation))
            setTransactionIsolationLevel(Connection.TRANSACTION_READ_COMMITTED);
        else
        if (FBConnectionHelper.TRANSACTION_REPEATABLE_READ.equalsIgnoreCase(isolation))
            setTransactionIsolationLevel(Connection.TRANSACTION_REPEATABLE_READ);
        else
        if (FBConnectionHelper.TRANSACTION_SERIALIZABLE.equalsIgnoreCase(isolation))
            setTransactionIsolationLevel(Connection.TRANSACTION_SERIALIZABLE);
        else
            throw new FBSQLException("Unknown transaction isolation.", 
                    FBSQLException.SQL_STATE_INVALID_ARG_VALUE);
    }

    private static final String REF_BLOCKING_TIMEOUT = "blockingTimeout";
    private static final String REF_IDLE_TIMEOUT = "idleTimeout";
    private static final String REF_LOGIN_TIMEOUT = "loginTimeout";
    private static final String REF_MAX_SIZE = "maxSize";
    private static final String REF_MIN_SIZE = "minSize";
    private static final String REF_PING_INTERVAL = "pingInterval";
    private static final String REF_TX_ISOLATION = "txIsolation";

    protected abstract BasicAbstractConnectionPool createObjectInstance();
    
    /**
     * Get object instance for the specified name in the specified context.
     * This method constructs new datasource if <code>obj</code> represents
     * {@link Reference}, whose factory class is equal to this class.
     */
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, 
                                    Hashtable environment) throws Exception 
    {
        
        if (!(obj instanceof Reference)) return null;
    
        Reference ref = (Reference)obj;
    
        if (!getClass().getName().equals(ref.getClassName()))
            return null;
    
        BasicAbstractConnectionPool ds = createObjectInstance();
        
        String addr;
    
        addr = getRefAddr(ref, REF_BLOCKING_TIMEOUT);
        if (addr != null)
            ds.setBlockingTimeout(Integer.parseInt(addr));
            
        addr = getRefAddr(ref, REF_IDLE_TIMEOUT);
        if (addr != null)
            ds.setIdleTimeout(Integer.parseInt(addr));
            
        addr = getRefAddr(ref, REF_LOGIN_TIMEOUT);
        if (addr != null)
            ds.setLoginTimeout(Integer.parseInt(addr));
            
        addr = getRefAddr(ref, REF_MAX_SIZE);
        if (addr != null)
            ds.setMaxConnections(Integer.parseInt(addr));
            
        addr = getRefAddr(ref, REF_MIN_SIZE);
        if (addr != null)
            ds.setMinConnections(Integer.parseInt(addr));
    
        addr = getRefAddr(ref, REF_PING_INTERVAL);
        if (addr != null)
            ds.setPingInterval(Integer.parseInt(addr));
        
        addr = getRefAddr(ref, REF_TX_ISOLATION);
        if (addr != null)
            ds.setTransactionIsolationLevel(Integer.parseInt(addr));
            
        return ds;
    }

    protected String getRefAddr(Reference ref, String type) {
        RefAddr addr = ref.get(type);
        if (addr == null)
            return null;
        else
            return addr.getContent().toString();
    }

    /**
     * Get JDNI reference.
     * 
     * @return instance of {@link Reference}.
     */
    public Reference getReference() {
        if (reference == null)
            return getDefaultReference();
        else
            return reference;
    }

    /**
     * Set JNDI reference for this data source.
     * 
     * @param reference JNDI reference.
     */
    public void setReference(Reference reference) {
        this.reference = reference;
    }

    /**
     * Get default JNDI reference for this datasource. This method is called if
     * datasource is used in non-JCA environment.
     * 
     * @return instance of {@link Reference} containing all information 
     * that allows to reconstruct the datasource.
     */
    public Reference getDefaultReference() {
        Reference ref = new Reference(getClass().getName());
        
        if (getBlockingTimeout() != FBPoolingDefaults.DEFAULT_BLOCKING_TIMEOUT)
            ref.add(new StringRefAddr(REF_BLOCKING_TIMEOUT, 
                String.valueOf(getBlockingTimeout())));
    
        if (getIdleTimeout() != FBPoolingDefaults.DEFAULT_IDLE_TIMEOUT)
            ref.add(new StringRefAddr(REF_IDLE_TIMEOUT,
                String.valueOf(getIdleTimeout())));
    
        if (getMaxConnections() != FBPoolingDefaults.DEFAULT_MAX_SIZE)
            ref.add(new StringRefAddr(REF_MAX_SIZE, 
                String.valueOf(getMaxConnections())));
    
        if (getMinConnections() != FBPoolingDefaults.DEFAULT_MIN_SIZE)
            ref.add(new StringRefAddr(REF_MIN_SIZE,
                String.valueOf(getMinConnections())));
            
        if (getPingInterval() != FBPoolingDefaults.DEFAULT_PING_INTERVAL)
            ref.add(new StringRefAddr(REF_PING_INTERVAL, 
                String.valueOf(getPingInterval())));
        
        if (getTransactionIsolationLevel() != FBPoolingDefaults.DEFAULT_ISOLATION)
            ref.add(new StringRefAddr(REF_TX_ISOLATION,
                String.valueOf(getTransactionIsolationLevel())));
            
        return ref;
    }

    protected byte[] serialize(Object obj) {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        
        try {
            ObjectOutputStream out = new ObjectOutputStream(bout);
            out.writeObject(obj);
            out.flush();
        } catch(IOException ex) {
            return null;
        }
        
        return bout.toByteArray();
    }

    protected Object deserialize(byte[] data) {
        ByteArrayInputStream bin = new ByteArrayInputStream(data);
        
        try {
            ObjectInputStream in = new ObjectInputStream(bin);
            return in.readObject();
        } catch(IOException ex) {
            return null;
        } catch(ClassNotFoundException ex) {
            return null;
        }
    }

    
}