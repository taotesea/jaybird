package org.firebirdsql.jdbc.oo;

import java.sql.*;

import org.firebirdsql.gds.GDSException;
import org.firebirdsql.jca.FBManagedConnection;
import org.firebirdsql.jdbc.*;

public class OOConnection extends FBConnection {

    private OODatabaseMetaData metaData;

    public OOConnection(FBManagedConnection mc) {
        super(mc);
    }

    public synchronized DatabaseMetaData getMetaData() throws SQLException {
        try {
            if (metaData == null) metaData = new OODatabaseMetaData(this);

            return metaData;
        } catch (GDSException ex) {
            throw new FBSQLException(ex);
        }
    }

    public synchronized Statement createStatement(int resultSetType,
            int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        try {
            Statement stmt = new OOStatement(getGDSHelper(), resultSetType,
                    resultSetConcurrency, resultSetHoldability, txCoordinator);

            activeStatements.add(stmt);
            return stmt;
        } catch (GDSException ex) {
            throw new FBSQLException(ex);
        }
    }

    public synchronized PreparedStatement prepareStatement(String sql,
            int resultSetType, int resultSetConcurrency,
            int resultSetHoldability, boolean metaData, boolean generatedKeys) throws SQLException {
        try {
            FBObjectListener.StatementListener coordinator = txCoordinator;
            if (metaData)
                coordinator = new InternalTransactionCoordinator.MetaDataTransactionCoordinator(
                        txCoordinator);

            FBObjectListener.BlobListener blobCoordinator;
            if (metaData)
                blobCoordinator = null;
            else
                blobCoordinator = txCoordinator;

            PreparedStatement stmt = new OOPreparedStatement(getGDSHelper(),
                    sql, resultSetType, resultSetConcurrency,
                    resultSetHoldability, coordinator, blobCoordinator,
                    metaData, false, generatedKeys);

            activeStatements.add(stmt);
            return stmt;

        } catch (GDSException ex) {
            throw new FBSQLException(ex);
        }
    }
}