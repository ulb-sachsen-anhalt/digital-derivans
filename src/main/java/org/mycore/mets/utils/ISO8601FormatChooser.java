/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.mets.utils;

import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;

public final class ISO8601FormatChooser {

    public static final DateTimeFormatter COMPLETE_HH_MM_FORMAT = ISODateTimeFormat.dateHourMinute();

    public static final DateTimeFormatter COMPLETE_HH_MM_SS_FORMAT = ISODateTimeFormat.dateTimeNoMillis();

    public static final DateTimeFormatter COMPLETE_HH_MM_SS_SSS_FORMAT = ISODateTimeFormat.dateTime();

    private static final Pattern MILLI_CHECK_PATTERN = Pattern.compile("\\.\\d{4,}");

    /**
     * returns a DateTimeFormatter for the given isoString or format. This method prefers the format parameter. So if
     * it's not null or not zero length this method will interpret the format string. You can also get a formatter for e
     * specific iso String. In either case if the underlying algorithm can not determine an exact matching formatter it
     * will allway fall back to a default. So this method will never return null.
     *
     * @param isoString
     *            an ISO 8601 formatted time String, or null
     * @return returns a specific DateTimeFormatter
     */
    public static DateTimeFormatter getFormatter(String isoString) {
        DateTimeFormatter df;
        if (isoString != null && isoString.length() != 0) {
            df = getFormatterForDuration(isoString);
        } else {
            df = COMPLETE_HH_MM_SS_SSS_FORMAT;
        }
        return df;
    }

    private static DateTimeFormatter getFormatterForDuration(String isoString) {
        boolean test = false;
        switch (isoString.length()) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 10:
            case 11:
                throw new IllegalArgumentException("no dateTime: " + isoString);
            case 17: // YYYY-MM-DDThh:mm'Z'
                test = true;
            case 22:
                if (test || !isoString.endsWith("Z")) {
                    // YYYY-MM-DDThh:mm[+-]hh:mm
                    return COMPLETE_HH_MM_FORMAT;
                }
                // YYYY-MM-DDThh:mm:ss.s'Z'
                return COMPLETE_HH_MM_SS_SSS_FORMAT;
            case 20: // YYYY-MM-DDThh:mm:ss'Z'
            case 25: // YYYY-MM-DDThh:mm:ss[+-]hh:mm
                return COMPLETE_HH_MM_SS_FORMAT;
            case 23: // YYYY-MM-DDThh:mm:ss.ss'Z'
            case 24: // YYYY-MM-DDThh:mm:ss.sss'Z'
            case 27: // YYYY-MM-DDThh:mm:ss.s[+-]hh:mm
            case 28: // YYYY-MM-DDThh:mm:ss.ss[+-]hh:mm
            case 29: // YYYY-MM-DDThh:mm:ss.ss[+-]hh:mm
                return COMPLETE_HH_MM_SS_SSS_FORMAT;
            default:
                return COMPLETE_HH_MM_SS_SSS_FORMAT;
        }
    }

    /**
     * returns a String that has not more than 3 digits representing "fractions of a second". If isoString has no or not
     * more than 3 digits this method simply returns isoString.
     *
     * @param isoString
     *            an ISO 8601 formatted time String
     * @return an ISO 8601 formatted time String with at max 3 digits for fractions of a second
     */
    public static String cropSecondFractions(String isoString) {
        Matcher matcher = MILLI_CHECK_PATTERN.matcher(isoString);
        boolean result = matcher.find();
        if (result) {
            return matcher.replaceFirst(isoString.substring(matcher.start(), matcher.start() + 4));
        }
        return isoString;
    }

    private static class ISODateTimeFormat {

        public static DateTimeFormatter dateHourMinute() {
            return new DateTimeFormatterBuilder().parseCaseInsensitive()
                .appendValue(YEAR, 4)
                .appendLiteral('-')
                .appendValue(MONTH_OF_YEAR, 2)
                .appendLiteral('-')
                .appendValue(DAY_OF_MONTH, 2)
                .appendLiteral('T')
                .appendValue(ChronoField.HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                .optionalStart()
                .appendOffset("+HH:MM", "Z")
                .optionalEnd()
                .toFormatter(Locale.ROOT)
                .withChronology(IsoChronology.INSTANCE)
                .withResolverStyle(ResolverStyle.STRICT);
        }

        public static DateTimeFormatter dateTimeNoMillis() {
            return new DateTimeFormatterBuilder().parseCaseInsensitive()
                .appendValue(YEAR, 4)
                .appendLiteral('-')
                .appendValue(MONTH_OF_YEAR, 2)
                .appendLiteral('-')
                .appendValue(DAY_OF_MONTH, 2)
                .appendLiteral('T')
                .appendValue(ChronoField.HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                .appendLiteral(':')
                .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
                .optionalStart()
                .appendOffset("+HH:MM", "Z")
                .optionalEnd()
                .toFormatter(Locale.ROOT)
                .withChronology(IsoChronology.INSTANCE)
                .withResolverStyle(ResolverStyle.STRICT);
        }

        public static DateTimeFormatter dateTime() {
            return new DateTimeFormatterBuilder().parseCaseInsensitive()
                .appendValue(YEAR, 4)
                .appendLiteral('-')
                .appendValue(MONTH_OF_YEAR, 2)
                .appendLiteral('-')
                .appendValue(DAY_OF_MONTH, 2)
                .appendLiteral('T')
                .appendValue(ChronoField.HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                .appendLiteral(':')
                .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
                .optionalStart()
                .appendFraction(ChronoField.NANO_OF_SECOND, 3, 3, true)
                .appendOffset("+HH:MM", "Z")
                .optionalEnd()
                .toFormatter(Locale.ROOT)
                .withChronology(IsoChronology.INSTANCE)
                .withResolverStyle(ResolverStyle.STRICT);
        }
    }

}
