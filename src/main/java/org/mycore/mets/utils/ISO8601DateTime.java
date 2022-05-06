package org.mycore.mets.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Objects;

public class ISO8601DateTime {

    private DateTimeFormatter dateTimeFormatter = ISO8601FormatChooser.getFormatter(null);

    private TemporalAccessor dt;

    /**
     * @param isoString
     *            a date or dateTime string as defined on <a href="http://www.w3.org/TR/NOTE-datetime">W3C Page</a>
     */
    public ISO8601DateTime(final String isoString) {
        setDateTime(isoString);
    }

    /**
     * returns the point in time representing this element.
     */
    public final TemporalAccessor getTemporalAccessor() {
        return dt;
    }

    /**
     * returns a ISO 8601 conform String using the current set format.
     *
     * @return date in ISO 8601 format, or null if date is unset.
     */
    public final String getISOString() {
        return dt == null ? null : dateTimeFormatter.format(dt);
    }

    /**
     * sets the date for this meta data object
     *
     * @param isoString
     *            Date in any form that is a valid W3C dateTime
     */
    private final void setDateTime(final String isoString) {
        final String timeString = ISO8601FormatChooser.cropSecondFractions(isoString);
        TemporalAccessor dt = getDateTime(timeString);
        setInstant(dt);
    }

    private TemporalAccessor getDateTime(final String timeString) {
        dateTimeFormatter = ISO8601FormatChooser.getFormatter(timeString);
        return dateTimeFormatter.parseBest(timeString, ZonedDateTime::from, LocalDateTime::from, LocalDate::from,
            YearMonth::from,
            Year::from);
    }

    private void setInstant(final TemporalAccessor dt) {
        this.dt = dt;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ISO8601DateTime other = (ISO8601DateTime) obj;
        return Objects.equals(this.dt, other.dt);
    }

}
