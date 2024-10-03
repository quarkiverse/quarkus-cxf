package io.quarkiverse.cxf.deployment.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.test.QuarkusUnitTest;

public class PerClientOrServiceSensitiveTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloService.class, HelloServiceImpl.class, Person.class, ServiceAddHeader.class,
                            ClientAddHeader.class))

            /* Service */
            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".implementor",
                    HelloServiceImpl.class.getName())
            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".logging.enabled", "true")
            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".out-interceptors", "#ServiceAddHeader")
            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".logging.sensitive-element-names", "firstName,greetingPrefix")
            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".logging.sensitive-protocol-header-names",
                    "Service-Secret-Header")

            .overrideConfigKey("quarkus.cxf.client.hello.client-endpoint-url", "http://localhost:8081/services/hello")
            .overrideConfigKey("quarkus.cxf.client.hello.out-interceptors", "#ClientAddHeader")
            .overrideConfigKey("quarkus.cxf.client.hello.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.hello.logging.enabled", "true")
            .overrideConfigKey("quarkus.cxf.client.hello.logging.sensitive-element-names", "surname,greetingSuffix")
            .overrideConfigKey("quarkus.cxf.client.hello.logging.sensitive-protocol-header-names", "Client-Secret-Header")

            .setLogRecordPredicate(logRecord -> logRecord.getLoggerName().contains("HelloService.RE")) // REQ_[IN|OUT] or RESP_[IN|OUT]
            .assertLogRecords(records ->

            assertThat(records)
                    .extracting(LogRecord::getMessage)
                    .anyMatch(messageExists("REQ_OUT", "Client-Secret-Header=XXX",
                            "<person><firstName>Joe</firstName><surname>XXX</surname></person>"))
                    .anyMatch(messageExists("REQ_IN", "Client-Secret-Header=client secret",
                            "<person><firstName>XXX</firstName><surname>Doe</surname></person>"))
                    .anyMatch(messageExists("RESP_OUT", "Service-Secret-Header=XXX",
                            "<return><greetingPrefix>XXX</greetingPrefix><greetingSuffix>Joe Doe</greetingSuffix></return>"))
                    .anyMatch(messageExists("RESP_IN", "Service-Secret-Header=service secret",
                            "<return><greetingPrefix>Hello</greetingPrefix><greetingSuffix>XXX</greetingSuffix></return>"))
                    .hasSize(4));

    ;

    @CXFClient("hello")
    HelloService hello;

    @Test
    void sensitive() {
        Assertions.assertThat(hello.hello(new Person("Joe", "Doe"))).isEqualTo(new Greeting("Hello", "Joe Doe"));
    }

    static Predicate<String> messageExists(String messageKind, String headersSubstring, String payloadSubstring) {
        return msg -> Pattern.compile(
                "^" + messageKind + ".*Headers: \\{[^\\}]*" + headersSubstring + "[^\\}]*\\}.*Payload: .*" + payloadSubstring
                        + ".*$",
                Pattern.DOTALL).matcher(msg).matches();
    }

    @WebService
    public interface HelloService {

        @WebMethod
        Greeting hello(@WebParam(name = "person") Person person);

    }

    @WebService(serviceName = "HelloService")
    public static class HelloServiceImpl implements HelloService {

        @Override
        public Greeting hello(Person person) {
            return new Greeting("Hello", person.firstName + " " + person.surname);
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "person")
    public static class Person {

        @XmlElement
        private String firstName;

        @XmlElement
        private String surname;

        public Person() {
        }

        public Person(String firstName, String surname) {
            this.firstName = firstName;
            this.surname = surname;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String name) {
            this.firstName = name;
        }

        public String getSurname() {
            return surname;
        }

        public void setSurname(String description) {
            this.surname = description;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Person)) {
                return false;
            }

            Person other = (Person) obj;
            return Objects.equals(other.firstName, this.firstName) && Objects.equals(other.surname, this.surname);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.firstName, this.surname);
        }

        @Override
        public String toString() {
            return firstName + " " + surname;
        }

    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "greeting")
    public static class Greeting {

        @XmlElement
        private String greetingPrefix;

        @XmlElement
        private String greetingSuffix;

        public Greeting() {
        }

        public Greeting(String firstName, String surname) {
            this.greetingPrefix = firstName;
            this.greetingSuffix = surname;
        }

        public String getGreetingPrefix() {
            return greetingPrefix;
        }

        public void setGreetingPrefix(String name) {
            this.greetingPrefix = name;
        }

        public String getGreetingSuffix() {
            return greetingSuffix;
        }

        public void setGreetingSuffix(String description) {
            this.greetingSuffix = description;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Greeting)) {
                return false;
            }

            Greeting other = (Greeting) obj;
            return Objects.equals(other.greetingPrefix, this.greetingPrefix)
                    && Objects.equals(other.greetingSuffix, this.greetingSuffix);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.greetingPrefix, this.greetingSuffix);
        }

        @Override
        public String toString() {
            return greetingPrefix + " " + greetingSuffix;
        }

    }

    static void addHeader(Message message, String headerName, String headerValue) {
        Map<String, List<String>> headers = (Map<String, List<String>>) message.get(Message.PROTOCOL_HEADERS);
        if (headers == null) {
            headers = new LinkedHashMap<>();
            message.put(Message.PROTOCOL_HEADERS, headers);
        }
        headers.put(headerName, Collections.singletonList(headerValue));
    }

    @ApplicationScoped
    @Named("ClientAddHeader")
    public static class ClientAddHeader extends AbstractPhaseInterceptor<Message> {

        public ClientAddHeader() {
            super(Phase.PREPARE_SEND);
        }

        @Override
        public void handleMessage(Message message) throws Fault {
            addHeader(message, "Client-Secret-Header", "client secret");
        }

    }

    @ApplicationScoped
    @Named("ServiceAddHeader")
    public static class ServiceAddHeader extends AbstractPhaseInterceptor<Message> {

        public ServiceAddHeader() {
            super(Phase.PREPARE_SEND);
        }

        @Override
        public void handleMessage(Message message) throws Fault {
            addHeader(message, "Service-Secret-Header", "service secret");
        }

    }
}
