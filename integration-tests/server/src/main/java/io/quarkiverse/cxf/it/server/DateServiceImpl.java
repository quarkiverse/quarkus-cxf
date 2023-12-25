package io.quarkiverse.cxf.it.server;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import jakarta.jws.WebService;

/**
 * The simplest Hello service implementation.
 */
@WebService(serviceName = "DateService")
public class DateServiceImpl implements DateService {

    @Override
    public Calendar calendarAdd(Calendar base, int days) {
        Calendar result = (Calendar) base.clone();
        result.add(Calendar.DAY_OF_YEAR, days);
        return result;
    }

    @Override
    public XMLGregorianCalendar xmlGregorianCalendarAdd(XMLGregorianCalendar base, int days) {
        XMLGregorianCalendar result = (XMLGregorianCalendar) base.clone();
        try {
            result.add(DatatypeFactory.newInstance().newDuration(true, 0, 0, days, 0, 0, 0));
            return result;
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public LocalDateTime localDateTimeAdd(LocalDateTime base, int days) {
        return base.plus(days, ChronoUnit.DAYS);
    }

}
