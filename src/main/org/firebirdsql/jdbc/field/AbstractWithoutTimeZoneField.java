/*
 * Firebird Open Source JavaEE Connector - JDBC Driver
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
 * can be obtained from a source control history command.
 *
 * All rights reserved.
 */
package org.firebirdsql.jdbc.field;

import org.firebirdsql.gds.ng.fields.FieldDescriptor;

import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Common superclass for {@link FBTimeField} and {@link FBTimestampField} to handle session time zone.
 *
 * @author <a href="mailto:mrotteveel@users.sourceforge.net">Mark Rotteveel</a>
 */
abstract class AbstractWithoutTimeZoneField extends FBField {

    private Calendar calendar;

    AbstractWithoutTimeZoneField(FieldDescriptor fieldDescriptor, FieldDataProvider dataProvider, int requiredType)
            throws SQLException {
        super(fieldDescriptor, dataProvider, requiredType);
    }

    @Override
    public final Time getTime() throws SQLException {
        if (isNull()) return null;

        return getTime(getCalendar());
    }

    @Override
    public final Timestamp getTimestamp() throws SQLException {
        if (isNull()) return null;

        return getTimestamp(getCalendar());
    }

    @Override
    public final void setTime(Time value) throws SQLException {
        if (value == null) {
            setNull();
            return;
        }

        setTime(value, getCalendar());
    }

    @Override
    public final void setTimestamp(Timestamp value) throws SQLException {
        if (value == null) {
            setNull();
            return;
        }

        setTimestamp(value, getCalendar());
    }

    Calendar getCalendar() {
        if (calendar == null) {
            return initCalendar();
        }
        return calendar;
    }

    private Calendar initCalendar() {
        TimeZone sessionTimeZone = gdsHelper != null ? gdsHelper.getSessionTimeZone() : null;
        return calendar = sessionTimeZone != null ? Calendar.getInstance(sessionTimeZone) : Calendar.getInstance();
    }

}