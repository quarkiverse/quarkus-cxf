package io.quarkiverse.cxf.it.server;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Locale;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ServerTest {

    @Test
    public void fault() {
        final FaultyHelloService client = QuarkusCxfClientTestUtil.getClient(FaultyHelloService.class, "/soap/faulty-hello");
        Assertions.assertThatThrownBy(() -> {
            client.faultyHello("Joe");
        }).isInstanceOf(GreetingException.class);
    }

    @Test
    public void calendarAdd() {
        final DateService dateService = QuarkusCxfClientTestUtil.getClient(DateService.class, "/soap/date");
        final Calendar base = Calendar.getInstance(java.util.TimeZone.getTimeZone("GMT"), Locale.US);
        base.setTimeInMillis(0L);
        final Calendar result = dateService.calendarAdd(base, 1);
        Assertions.assertThat(result.getTimeInMillis()).isEqualTo(1000L * 60 * 60 * 24);
    }

    @Test
    public void xmlGregorianCalendarAdd() throws DatatypeConfigurationException {
        final DateService dateService = QuarkusCxfClientTestUtil.getClient(DateService.class, "/soap/date");
        final LocalDateTime b = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC);
        final XMLGregorianCalendar base = DatatypeFactory.newInstance()
                .newXMLGregorianCalendar(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(b));
        final XMLGregorianCalendar actual = dateService.xmlGregorianCalendarAdd(base, 1);
        final XMLGregorianCalendar expected = DatatypeFactory.newInstance()
                .newXMLGregorianCalendar(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(b.plusDays(1)));
        Assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void localDateTimeAdd() {
        final DateService dateService = QuarkusCxfClientTestUtil.getClient(DateService.class, "/soap/date");
        final LocalDateTime base = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC);
        final int days = 1;
        final LocalDateTime result = dateService.localDateTimeAdd(base, days);
        Assertions.assertThat(result.minusDays(days)).isEqualTo(base);
    }

}
