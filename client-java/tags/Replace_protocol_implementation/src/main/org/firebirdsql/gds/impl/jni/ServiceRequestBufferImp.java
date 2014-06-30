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
package org.firebirdsql.gds.impl.jni;

import org.firebirdsql.gds.ServiceRequestBuffer;
import org.firebirdsql.gds.impl.argument.NumericArgument;
import org.firebirdsql.gds.impl.argument.StringArgument;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * ngds implementation for ServiceRequestBufferImp.
 */
class ServiceRequestBufferImp extends ParameterBufferBase implements
        ServiceRequestBuffer {

    /**
     * Every ServiceRequestBuffer has an associated taskIdentifier.
     * 
     * @param taskIdentifier
     */
    ServiceRequestBufferImp(int taskIdentifier) {
        this.taskIdentifier = taskIdentifier;
    }

    @Override
    public void addArgument(int argumentType, String value) {
        getArgumentsList().add(new StringArgument(argumentType, value) {

            @Override
            public int getLength() {
                return super.getLength() + 1;
            }

            @Override
            protected void writeLength(int length, OutputStream outputStream) throws IOException {
                outputStream.write(length);
                outputStream.write(length >> 8);
            }

            @Override
             protected int getMaxSupportedLength() {
                // TODO Check if this might be signed
                return 65535;
            }
        });
    }
    
    @Override
    public void addArgument(int argumentType, int value) {
        getArgumentsList().add(new NumericArgument(argumentType, value) {

            @Override
            public int getLength() {
                return 5;
            }

            @Override
            protected void writeValue(OutputStream outputStream, int value) throws IOException {
                outputStream.write(value);
                outputStream.write(value>>8);
                outputStream.write(value>>16);
                outputStream.write(value>>24);
            }
        });
    }
    
    public void addArgument(int argumentType, byte value) {
        getArgumentsList().add(new NumericArgument(argumentType, value) {

            @Override
            public int getLength() {
                return 2;
            }
            
            @Override
            protected void writeValue(OutputStream outputStream, final int value) throws IOException {
                outputStream.write(value);
            }
        });
    }

    /**
     * Package local method for obtaining buffer suitable for passing to native
     * method.
     * 
     * @return
     */
    byte[] toByteArray() {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(taskIdentifier);

        try {
            super.writeArgumentsTo(byteArrayOutputStream);
        } catch (IOException e) {
            // Ignoring IOException, not thrown by ByteArrayOutputStream
        }

        return byteArrayOutputStream.toByteArray();
    }

    // PRIVATE MEMBERS

    private final int taskIdentifier;
}