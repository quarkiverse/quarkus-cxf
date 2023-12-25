package io.quarkiverse.cxf.it.server;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

import javax.xml.datatype.XMLGregorianCalendar;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import jakarta.xml.ws.RequestWrapper;
import jakarta.xml.ws.ResponseWrapper;

/**
 * Some date operations
 */
@WebService(name = "DateService", serviceName = "DateService")
public interface DateService {

    @WebMethod
    Calendar calendarAdd(Calendar base, int days);

    @WebMethod
    XMLGregorianCalendar xmlGregorianCalendarAdd(XMLGregorianCalendar base, int days);

    @WebMethod
    @RequestWrapper(className = "io.quarkiverse.cxf.it.server.DateService$DateTimeRequest")
    @ResponseWrapper(className = "io.quarkiverse.cxf.it.server.DateService$DateTimeResponse")
    @WebResult(name = "result")
    LocalDateTime localDateTimeAdd(
            @WebParam(name = "base") LocalDateTime base,
            @WebParam(name = "days") int days);

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "DateTimeRequest", propOrder = {
            "base",
            "days"
    })
    public static class DateTimeRequest {

        @XmlJavaTypeAdapter(LocalDateTimeAdapter.class)
        private LocalDateTime base;

        private int days;

        public LocalDateTime getBase() {
            return base;
        }

        public void setBase(LocalDateTime result) {
            this.base = result;
        }

        public int getDays() {
            return days;
        }

        public void setDays(int days) {
            this.days = days;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "DateTimeResponse", propOrder = { "result" })
    public static class DateTimeResponse {

        @XmlJavaTypeAdapter(LocalDateTimeAdapter.class)
        private LocalDateTime result;

        public LocalDateTime getResult() {
            return result;
        }

        public void setResult(LocalDateTime result) {
            this.result = result;
        }
    }

    public static class LocalDateTimeAdapter extends XmlAdapter<String, LocalDateTime> {

        @Override
        public LocalDateTime unmarshal(String value) {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        @Override
        public String marshal(LocalDateTime value) {
            return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(value);
        }
    }
}
