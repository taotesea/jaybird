/*
 * $Id$
 *
 * Public Firebird Java API.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    1. Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.firebirdsql.gds.ng;

import org.firebirdsql.encodings.Encoding;
import org.firebirdsql.encodings.IEncodingFactory;
import org.firebirdsql.gds.DatabaseParameterBuffer;
import org.firebirdsql.gds.TransactionParameterBuffer;

import java.sql.SQLException;

/**
 * @author <a href="mailto:mrotteveel@users.sourceforge.net">Mark Rotteveel</a>
 * @since 2.3
 */
public interface FbDatabase {

    /**
     * Attach to a database.
     *
     * @param dpb
     *         The DatabaseParameterBuffer with all required values
     * @throws SQLException
     */
    void attach(DatabaseParameterBuffer dpb) throws SQLException;

    /**
     * Detaches from the current database.
     *
     * @throws SQLException
     */
    void detach() throws SQLException;

    /**
     * Creates a new database, connection remains attached to database.
     *
     * @param dpb
     *         DatabaseParameterBuffer with all required values
     * @throws SQLException
     */
    void createDatabase(DatabaseParameterBuffer dpb) throws SQLException;

    /**
     * Drops (and deletes) the currently attached database.
     *
     * @throws SQLException
     */
    void dropDatabase() throws SQLException;

    /**
     * Cancels the current operation.
     *
     * @param kind
     *         TODO Document parameter kind of cancelOperation
     * @throws SQLException
     *         For errors cancelling, or if the cancel operation is not supported.
     */
    void cancelOperation(int kind) throws SQLException;

    /**
     * Creates and starts a transaction.
     *
     * @param tpb
     *         TransactionParameterBuffer with the required transaction
     *         options
     * @return Transaction
     * @throws SQLException
     */
    FbTransaction createTransaction(TransactionParameterBuffer tpb) throws SQLException;

    /**
     * Creates a statement with an implicit transaction.
     *
     * @return GdsStatement with implicit transaction
     * @throws SQLException
     */
    FbStatement createStatement() throws SQLException;

    /**
     * Creates a statement associated with a transaction
     *
     * @param transaction
     *         GdsTransaction to associate with this statement
     * @return GdsStatement
     * @throws SQLException
     */
    FbStatement createStatement(FbTransaction transaction) throws SQLException;

    /**
     * Request database info.
     *
     * @param requestItems
     *         Array of info items to request
     * @param bufferLength
     *         Response buffer length to use
     * @param infoProcessor
     *         Implementation of {@link InfoProcessor} to transform
     *         the info response
     * @return Transformed info response of type T
     * @throws SQLException
     *         For errors retrieving or transforming the response.
     */
    <T> T getDatabaseInfo(byte[] requestItems, int bufferLength, InfoProcessor<T> infoProcessor)
            throws SQLException;

    /**
     * Performs a database info request.
     *
     * @param requestItems
     *         Information items to request
     * @param maxBufferLength
     *         Maximum response buffer length to use
     * @return The response buffer (note: length is the actual length of the
     *         response, not <code>maxBufferLength</code>
     * @throws SQLException
     *         For errors retrieving the information.
     */
    byte[] getDatabaseInfo(byte[] requestItems, int maxBufferLength) throws SQLException;

    /**
     * @return The database dialect
     */
    short getDatabaseDialect();

    /**
     * @return The client connection dialect
     */
    short getConnectionDialect();

    /**
     * @return The database handle value
     */
    int getHandle();

    /**
     * @return Number of open transactions
     */
    int getTransactionCount();

    /**
     * Sets the WarningMessageCallback for this database.
     *
     * @param callback
     *         WarningMessageCallback
     */
    void setWarningMessageCallback(WarningMessageCallback callback);

    /**
     * Current attachment status of the database.
     *
     * @return <code>true</code> if connected to the server and attached to a
     *         database, <code>false</code> otherwise.
     */
    boolean isAttached();

    /**
     * Get synchronization object.
     *
     * @return object, cannot be <code>null</code>.
     */
    Object getSynchronizationObject();

    /**
     * @return ODS major version
     */
    int getOdsMajor();

    /**
     * @return ODS minor version
     */
    int getOdsMinor();

    /**
     * @return Firebird version string
     */
    String getVersionString();

    /**
     * @return The {@link IEncodingFactory} for this connection
     */
    IEncodingFactory getEncodingFactory();

    /**
     * @return The connection encoding (should be the same as returned from calling {@link org.firebirdsql.encodings.IEncodingFactory#getDefaultEncoding()}
     * on the result of {@link #getEncodingFactory()}.
     */
    Encoding getEncoding();
}
