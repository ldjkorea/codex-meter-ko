package dev.bennett.codexmeter;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class V3Display {
    private V3Display(){}
    static String range(LedgerPeriods.Span span){return date(span.start)+" ~ "+date(span.end);}
    static String range(long start,long end){return date(start)+" ~ "+date(end);}
    private static String date(long at){return Instant.ofEpochMilli(at).atZone(LedgerAggregation.ZONE).format(DateTimeFormatter.ofPattern("MM.dd",Locale.getDefault()));}
    static String time(long at){return Instant.ofEpochMilli(at).atZone(LedgerAggregation.ZONE).format(DateTimeFormatter.ofPattern("MM.dd HH:mm",Locale.getDefault()));}
}
