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

package org.firebirdsql.jdbc;


import java.io.*;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.*;

import org.firebirdsql.gds.*;

/**
 * The representation (mapping) in
 * the Java<sup><font size=-2>TM</font></sup> programming
 * language of an SQL
 * <code>BLOB</code> value.  An SQL <code>BLOB</code> is a built-in type
 * that stores a Binary Large Object as a column value in a row of
 * a database table. The driver implements <code>Blob</code> using
 * an SQL <code>locator(BLOB)</code>, which means that a
 * <code>Blob</code> object contains a logical pointer to the
 * SQL <code>BLOB</code> data rather than the data itself.
 * A <code>Blob</code> object is valid for the duration of the
 * transaction in which is was created.
 *
 * <P>Methods in the interfaces {@link ResultSet},
 * {@link CallableStatement}, and {@link PreparedStatement}, such as
 * <code>getBlob</code> and <code>setBlob</code> allow a programmer to
 * access an SQL <code>BLOB</code> value.
 * The <code>Blob</code> interface provides methods for getting the
 * length of an SQL <code>BLOB</code> (Binary Large Object) value,
 * for materializing a <code>BLOB</code> value on the client, and for
 * determining the position of a pattern of bytes within a
 * <code>BLOB</code> value.
 *<P>
 * This class is new in the JDBC 2.0 API.
 * @since 1.2
 */

public class FBBlob implements FirebirdBlob, Synchronizable {
    
    private static final boolean SEGMENTED = true;
    public static final int READ_FULLY_BUFFER_SIZE = 16 * 1024;

    /**
     * bufferlength is the size of the buffer for blob input and output streams,
     * also used for the BufferedInputStream/BufferedOutputStream wrappers.
     *
     */
    private int bufferlength;

    private boolean isNew;
    private long blob_id;
    private AbstractConnection c;

    private Collection inputStreams = new HashSet();
    private FBBlobOutputStream blobOut = null;

    private FBBlob(AbstractConnection c, boolean isNew) {
        this.c = c;
        this.isNew = isNew;
        this.bufferlength = c.getBlobBufferLength().intValue();
    }

    /**
     * Create new Blob instance. This constructor creates new fresh Blob, only
     * writing to the Blob is allowed.
     * 
     * @param c connection that will be used to write data to blob.
     */
    public FBBlob(AbstractConnection c) {
        this(c, true);
    }

    /**
     * Create instance of this class to access existing Blob.
     * 
     * @param c connection that will be used to access Blob.
     * 
     * @param blob_id ID of the Blob.
     */
    public FBBlob(AbstractConnection c, long blob_id) {
        this(c, false);
        this.blob_id = blob_id;
    }
    
    /**
     * Get synchronization object that will be used to synchronize multithreaded
     * access to the database.
     * 
     * @return object that will be used for synchronization.
     */
    public Object getSynchronizationObject() {
        return c;
    }

    /**
     * Close this Blob object. This method closes all open input streams.
     * 
     * @throws IOException if at least one of the stream raised an exception
     * when closed.
     */
    public void close() throws IOException {
        
        IOException error = null;
        
        Iterator i = inputStreams.iterator();
        while (i.hasNext()) {
            try {
                ((FBBlobInputStream)i.next()).close();
            } catch(IOException ex) {
                error = ex;
            }
        }
        inputStreams.clear();
        
        if (error != null)
            throw error;
    }

    /**
     * Get information about this Blob. This method should be considered as 
     * temporary because it provides access to low-level API. More information
     * on how to use the API can be found in "API Guide".
     * 
     * @param items items in which we are interested.
     * @param buffer_length buffer where information will be stored.
     * 
     * @return array of bytes containing information about this Blob.
     * 
     * @throws SQLException if something went wrong.
     */
    public byte[] getInfo(byte[] items, int buffer_length) throws SQLException {
        
        Object syncObject = getSynchronizationObject();
        
        synchronized(syncObject) {
            try {
                c.ensureInTransaction();
                
                isc_blob_handle blob = c.openBlobHandle(blob_id, SEGMENTED);
                try {
                    GDS gds = c.getInternalAPIHandler();
                    return gds.isc_blob_info(blob, items, buffer_length);
                } finally {
                    c.closeBlob(blob);
                }
                
            } catch(GDSException ex) {
                throw new FBSQLException(ex);
            } finally {
                if (c.willEndTransaction())
                    c.checkEndTransaction();
            }
        }
    }

  /**
   * Returns the number of bytes in the <code>BLOB</code> value
   * designated by this <code>Blob</code> object.
   * @return length of the <code>BLOB</code> in bytes
   * @exception SQLException if there is an error accessing the
   * length of the <code>BLOB</code>
   * @since 1.2
   * @see <a href="package-summary.html#2.0 API">What Is in the JDBC 2.0 API</a>
   */
    public long length() throws SQLException {
        byte[] info = getInfo(
            new byte[]{ISCConstants.isc_info_blob_total_length}, 20);
            
        return interpretLength(info, 0);
    }

    /**
     * Interpret BLOB length from buffer.
     * 
     * @param info server response.
     * @param position where to start interpreting.
     * 
     * @return length of the blob.
     * 
     * @throws SQLException if length cannot be interpreted.
     */            
    private long interpretLength(byte[] info, int position) throws SQLException { 
                
        if (info[position] != ISCConstants.isc_info_blob_total_length)
            throw new SQLException("Length is not available.");
            
        int dataLength = 
            c.getInternalAPIHandler().isc_vax_integer(info, position + 1, 2);
            
        return c.getInternalAPIHandler().isc_vax_integer(
            info, position + 3, dataLength);
    }

    /**
     * Check if blob is segmented. 
     * 
     * @return <code>true</code> if this blob is segmented, 
     * otherwise <code>false</code>
     * 
     * @throws SQLException if something went wrong.
     */
    public boolean isSegmented() throws SQLException {
        byte[] info = getInfo(
            new byte[] {ISCConstants.isc_info_blob_type}, 20);

        if (info[0] != ISCConstants.isc_info_blob_type)
            throw new SQLException("Cannot determine BLOB type");

        int dataLength =
            c.getInternalAPIHandler().isc_vax_integer(info, 1, 2);

        int type = c.getInternalAPIHandler().isc_vax_integer(
            info, 3, dataLength);

        return type == ISCConstants.isc_bpb_type_segmented;
    }

    /**
     * Detach this blob. This method creates new instance of the same blob 
     * database object that is not under result set control. When result set
     * is closed, all associated resources are also released, including open
     * blob streams. This method creates an new instance of blob object with
     * the same blob ID that can be used even when result set is closed.
     * <p>
     * Note, detached blob will not remember the stream position of this object.
     * This means that you cannot start reading data from the blob, then detach
     * it, and then continue reading. Reading from detached blob will begin at
     * the blob start.
     * 
     * @return instance of {@link FBBlob} that is not under result set control.
     * 
     * @throws SQLException if Blob cannot be detached.
     */    
    public FirebirdBlob detach() throws SQLException {
        return new FBBlob(c, blob_id);
    }

  /**
   * Returns as an array of bytes, part or all of the <code>BLOB</code>
   * value that this <code>Blob</code> object designates.  The byte
   * array contains up to <code>length</code> consecutive bytes
   * starting at position <code>pos</code>.
   * @param pos the ordinal position of the first byte in the
   * <code>BLOB</code> value to be extracted; the first byte is at
   * position 1
   * @param length the number of consecutive bytes to be copied
   * @return a byte array containing up to <code>length</code>
   * consecutive bytes from the <code>BLOB</code> value designated
   * by this <code>Blob</code> object, starting with the
   * byte at position <code>pos</code>
   * @exception SQLException if there is an error accessing the
   * <code>BLOB</code>
   * @since 1.2
   * @see <a href="package-summary.html#2.0 API">What Is in the JDBC 2.0 API</a>
   */
    public byte[] getBytes(long pos, int length) throws SQLException{
        
        if (pos > Integer.MAX_VALUE)
            throw new SQLException("Blob position is limited to 2^31 - 1 " + 
                "due to isc_seek_blob limitations.");
        
        Object syncObject = getSynchronizationObject();
        synchronized(syncObject) {
            c.ensureInTransaction();
            
            try {
                FirebirdBlob.BlobInputStream in = 
                    (FirebirdBlob.BlobInputStream)getBinaryStream();
                    
                try {
                    byte[] result = new byte[length];
                    
                    in.seek((int)pos - 1);
                    in.readFully(result);
                    
                    return result;
                } finally {
                    in.close();
                }
                
            } catch(IOException ex) {
                throw new FBSQLException(ex);                    
            } finally {
                if (c.willEndTransaction())
                    c.checkEndTransaction();
            }
        }
     }


  /**
   * Retrieves the <code>BLOB</code> designated by this
   * <code>Blob</code> instance as a stream.
   * @return a stream containing the <code>BLOB</code> data
   * @exception SQLException if there is an error accessing the
   * <code>BLOB</code>
   * @since 1.2
   * @see <a href="package-summary.html#2.0 API">What Is in the JDBC 2.0 API</a>
   */
    public InputStream getBinaryStream () throws SQLException {
        FBBlobInputStream blobstream = new FBBlobInputStream(this);
        inputStreams.add(blobstream);
        return blobstream;
    }

  /**
   * Determines the byte position at which the specified byte
   * <code>pattern</code> begins within the <code>BLOB</code>
   * value that this <code>Blob</code> object represents.  The
   * search for <code>pattern</code> begins at position
   * <code>start</code>.
   * @param pattern the byte array for which to search
   * @param start the position at which to begin searching; the
   *        first position is 1
   * @return the position at which the pattern appears, else -1
   * @exception SQLException if there is an error accessing the
   * <code>BLOB</code>
   * @since 1.2
   * @see <a href="package-summary.html#2.0 API">What Is in the JDBC 2.0 API</a>
   */
    public long position(byte pattern[], long start) throws SQLException {
        throw new SQLException("Not yet implemented");
    }


  /**
   * Determines the byte position in the <code>BLOB</code> value
   * designated by this <code>Blob</code> object at which
   * <code>pattern</code> begins.  The search begins at position
   * <code>start</code>.
   * @param pattern the <code>Blob</code> object designating
   * the <code>BLOB</code> value for which to search
   * @param start the position in the <code>BLOB</code> value
   *        at which to begin searching; the first position is 1
   * @return the position at which the pattern begins, else -1
   * @exception SQLException if there is an error accessing the
   * <code>BLOB</code>
   * @since 1.2
   * @see <a href="package-summary.html#2.0 API">What Is in the JDBC 2.0 API</a>
   */
    public long position(Blob pattern, long start) throws SQLException {
        throw new SQLException("Not yet implemented");
    }


    //jdbc 3.0 additions

    /**
     *
     * @param param1 <description>
     * @exception java.sql.SQLException <description>
     */
    public void truncate(long param1) throws SQLException {
        throw new SQLException("Not yet implemented");
    }

    /**
     *
     * @param param1 <description>
     * @param param2 <description>
     * @return <description>
     * @exception java.sql.SQLException <description>
     */
    public int setBytes(long param1, byte[] param2) throws SQLException {
        throw new SQLException("Not yet implemented");
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
    public int setBytes(long param1, byte[] param2, int param3, int param4) throws SQLException {
        throw new SQLException("Not yet implemented");
    }

    /**
     *
     * @param pos The position in the blob to start writing.
     * @return OuputStream to write to.
     * @exception java.sql.SQLException <description>
     */
    public OutputStream setBinaryStream(long pos) throws SQLException {
        if (blobOut != null) {
            throw new SQLException("only one blob output stream open at a time!");
        }
        if (pos < 0) {
            throw new SQLException("You can't start before the beginning of the blob");
        }
        if ((isNew) && (pos > 0)) {
            throw new SQLException("previous value was null, you must start at position 0");
        }
        blobOut = new FBBlobOutputStream();
        if (pos > 0) {
            //copy pos bytes from input to output
            //implement this later
        }
        
        return blobOut;
    }


    //package methods

    public long getBlobId() throws SQLException {
        if (isNew) 
            throw new SQLException("No Blob ID is available in new Blob object.");

        return blob_id;
    }
    
    void setBlobId(long blob_id) {
        this.blob_id = blob_id;
        this.isNew = false;
    }

    public void copyStream(InputStream inputStream, int length) throws SQLException {
        OutputStream os = setBinaryStream(0);
        byte[] buffer = new byte[bufferlength];
        int chunk;
        try {
            while (length >0) {
                chunk =inputStream.read(buffer, 0, ((length<bufferlength) ? length:bufferlength));
                os.write(buffer, 0, chunk);
                length -= chunk;
            }
            os.close();
        }
        catch (IOException ioe) {
            throw new SQLException("read/write blob problem: " + ioe);
        }
    }

    public void copyCharacterStream(Reader inputStream, int length) throws SQLException {
        OutputStream os = setBinaryStream(0);
        OutputStreamWriter osw = new OutputStreamWriter(os);
        char[] buffer = new char[bufferlength];
        int chunk;
        try {
            while (length >0) {
                chunk =inputStream.read(buffer, 0, ((length<bufferlength) ? length:bufferlength));
                osw.write(buffer, 0, chunk);                
                length -= chunk;
            }
            os.close();
        }
        catch (IOException ioe) {
            throw new SQLException("read/write blob problem: " + ioe);
        }
    }


    //Inner classes

    public class FBBlobInputStream extends InputStream 
        implements FirebirdBlob.BlobInputStream
    {


        /**
         * buffer holds the last result of calling isc_get_segment.
         *
         */
        private byte[] buffer = null;


        /**
         * blob is the isc_blob_handle actually refencing the database;
         *
         */
        private isc_blob_handle blob;


        /**
         * pos is the position of the next byte to read in the buffer.
         *
         */
        private int pos = 0;
        
        private boolean closed;
        
        private FBBlob owner;

        private FBBlobInputStream(FBBlob owner) throws SQLException {
            this.owner = owner;
            
            closed = false;
            
            if (isNew) {
                throw new SQLException("You can't read a new blob");
            }
            
            Object syncObject = getSynchronizationObject();
            
            synchronized(syncObject) {
                try {
                    blob = c.openBlobHandle(blob_id, SEGMENTED);
                } catch (GDSException ge) {
                    throw new FBSQLException(ge);
                }
            }
        }
        
        public FirebirdBlob getBlob() {
            return owner;
        }

        public void seek(int position) throws IOException {
            seek(position, SEEK_MODE_ABSOLUTE);
        }

        public void seek(int position, int seekMode) throws IOException {
            
            Object syncObject = getSynchronizationObject();
            
            synchronized(syncObject) {
                try {
                    c.getInternalAPIHandler().isc_seek_blob(blob, position, seekMode);
                } catch (GDSException ex) {
                    /** @todo fix this */
                    throw new IOException(ex.getMessage());
                }
            }
        }
        
        public long length() throws IOException {
            
            Object syncObject = getSynchronizationObject();
            
            synchronized(syncObject) {
                try {
                    byte[] info = c.getInternalAPIHandler().isc_blob_info(
                        blob, new byte[] {ISCConstants.isc_info_blob_total_length}, 20);

                    return interpretLength(info, 0);
                } catch (GDSException ex) {
                    throw new IOException(ex.getMessage());
                } catch (SQLException ex) {
                    throw new IOException(ex.getMessage());
                }
            }
        }

        public int available() throws IOException {
            checkClosed();
            if (buffer == null) {
                if (blob.isEof()) {
                    return -1;
                }
                
                Object syncObject = getSynchronizationObject();
                synchronized(syncObject) {
                    try {
                        //bufferlength is in FBBlob enclosing class
                        buffer = c.getBlobSegment(blob, bufferlength);
                    } catch (GDSException ge) {
                        throw new IOException("Blob read problem: " +
                            ge.toString());
                    }
                }
                
                pos = 0;
                if (buffer.length == 0) {
                   return -1;
                }
            }
            return buffer.length - pos;
        }

        public int read() throws IOException {
            checkClosed();
            if (available() <= 0) {
                return -1;
            }
            int result = buffer[pos++] & 0x00FF;//& seems to convert signed byte to unsigned byte
            if (pos == buffer.length) {
                buffer = null;
            }
            return result;
        }

        public int read(byte[] b, int off, int len) throws IOException {
            checkClosed();
            int result = available();
            if (result <= 0) {
                return -1;
            }
            if (result > len) {//not expected to happen
                System.arraycopy(buffer, pos, b, off, len);
                pos += len;
                return len;
            }
            System.arraycopy(buffer, pos, b, off, result);
            buffer = null;
            pos = 0;
            return result;
        }
        
        public void readFully(byte[] b, int off, int len) throws IOException {
            int counter = 0;
            int pos = 0;
            byte[] buffer = new byte[READ_FULLY_BUFFER_SIZE];

            int toRead = len;

            while(toRead > 0 && (counter = read(buffer, 0, toRead)) != -1) {
                System.arraycopy(buffer, 0, b, pos, counter);
                pos += counter;
                
                toRead -= counter;
            }
            
            if (counter == -1)
                throw new EOFException();
        }
        
        public void readFully(byte[] b) throws IOException {
            readFully(b, 0, b.length);
        }

        public void close() throws IOException {
            
            Object syncObject = getSynchronizationObject();
            
            synchronized(syncObject) {
                if (blob != null) {
                    try {
                        c.closeBlob(blob);
                    } catch (GDSException ge) {
                        throw new IOException("couldn't close blob: " + ge);
                    }
                    blob = null;
                    closed = true;
                }
            }
        }
        
        private void checkClosed() throws IOException {
            if (closed) throw new IOException("Input stream is already closed.");
        }
    }

    public class FBBlobOutputStream extends OutputStream 
        implements FirebirdBlob.BlobOutputStream
    {

        private isc_blob_handle blob;

        private FBBlobOutputStream() throws SQLException {
            
            Object syncObject = getSynchronizationObject();
            
            synchronized(syncObject) {
                try {
                    DatabaseParameterBuffer dpb = c.getDatabaseParameterBuffer();
                    
                    boolean useStreamBlobs = 
                        dpb.hasArgument(DatabaseParameterBuffer.use_stream_blobs);
                    
                    blob = c.createBlobHandle(!useStreamBlobs);
                    
                } catch (GDSException ge) {
                    throw new FBSQLException(ge);
                }
            }
            
            if (isNew) {
                setBlobId(blob.getBlob_id());
            }
        }
        
        public void seek(int position, int seekMode) throws SQLException {
            try {
                c.getInternalAPIHandler().isc_seek_blob(blob, position, seekMode);
            } catch(GDSException ex) {
                throw new FBSQLException(ex);
            }
        }
        
        public long length() throws IOException {
            
            Object syncObject = getSynchronizationObject();
            
            synchronized(syncObject) {
                try {
                    byte[] info = c.getInternalAPIHandler().isc_blob_info(
                        blob, new byte[] {ISCConstants.isc_info_blob_total_length}, 20);

                    return interpretLength(info, 0);
                } catch (GDSException ex) {
                    throw new IOException(ex.getMessage());
                } catch (SQLException ex) {
                    throw new IOException(ex.getMessage());
                }
            }
        }

        public void write(int b) throws IOException {
            //This won't be called, don't implement
            throw new IOException("FBBlobOutputStream.write(int b) not implemented");
        }

        public void write(byte[] b, int off, int len) throws IOException {
            try {
                byte[] buf = new byte[bufferlength];
                int chunk;
                while (len > 0) {
                    if (len >= bufferlength) {
                        chunk = bufferlength;
                    }
                    else {
                        buf = new byte[len];
                        chunk = len;
                    }
                    System.arraycopy(b, off, buf, 0, chunk);
                    
                    Object syncObject = getSynchronizationObject();
                    
                    synchronized(syncObject) {
                        c.putBlobSegment(blob, buf);
                    }
                    
                    len -= chunk;
                }
            }
            catch (GDSException ge) {
                throw new IOException("Problem writing to FBBlobOutputStream: " + ge);
            }
        }

        public void close() throws IOException {
            if (blob != null) {
                try {
                    
                    Object syncObject = getSynchronizationObject();
                    
                    synchronized(syncObject) {
                        c.closeBlob(blob);
                    }
                    
                    setBlobId(blob.getBlob_id());
                    
                } catch (GDSException ge) {
                    throw new IOException("could not close blob: " + ge);
                }
                
                blob = null;
            }
        }

    }


}


