package io.quarkiverse.cxf.it.server;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;

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
    public LocalDateTime localDateTimeAdd(LocalDateTime base, int days) {
        return base.plus(days, ChronoUnit.DAYS);
    }

}
