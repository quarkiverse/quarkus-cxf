package io.quarkiverse.cxf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.wsdl.Binding;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.xml.namespace.QName;

/**
 * Finds the WSDL service a client should use in case the client's Service Endpoint Interface (SEI) does not specify any
 * service name.
 * <p>
 * The service name is not required by JAX-WS. When it is missing, the name is chosen by the server, so that the client
 * cannot derive it reliably from the SEI. Therefore, we look for the services whose ports are bound to the port type
 * corresponding to the SEI.
 */
final class WsdlServiceNameResolver {

    private WsdlServiceNameResolver() {
    }

    /**
     * @param definition the WSDL document to search
     * @param portTypeName the name of the port type corresponding to the client's SEI
     * @return an immutable list of names of services having at least one port whose binding refers to the given
     *         {@code portTypeName}, sorted by {@link QName#toString()}
     */
    static List<QName> findServicesByPortType(Definition definition, QName portTypeName) {
        final List<QName> result = new ArrayList<>();
        for (Object s : definition.getAllServices().values()) {
            final Service service = (Service) s;
            for (Object p : service.getPorts().values()) {
                final Binding binding = ((Port) p).getBinding();
                final PortType portType = binding != null ? binding.getPortType() : null;
                if (portType != null && portTypeName.equals(portType.getQName())) {
                    result.add(service.getQName());
                    break;
                }
            }
        }
        result.sort(Comparator.comparing(QName::toString));
        return List.copyOf(result);
    }

    /**
     * @param definition the WSDL document to search
     * @param portTypeName the name of the port type corresponding to the client's SEI
     * @param wsdlUrl the location of the {@code definition}, used in the exception message
     * @param clientDescription a human readable identification of the client, used in the exception message
     * @param sei the class name of the client's Service Endpoint Interface, used in the exception message
     * @return the name of the only service bound to the given {@code portTypeName}
     * @throws IllegalStateException if there is no such service or if there are multiple ones
     */
    static QName resolveServiceName(
            Definition definition,
            QName portTypeName,
            String wsdlUrl,
            String clientDescription,
            String sei) {
        final List<QName> services = findServicesByPortType(definition, portTypeName);
        if (services.size() == 1) {
            return services.get(0);
        }
        final String found = services.isEmpty()
                ? "no service having a port of type " + portTypeName
                : "multiple services having a port of type " + portTypeName + ": " + services;
        throw new IllegalStateException("Cannot determine the WSDL service for " + clientDescription
                + " because its Service Endpoint Interface " + sei
                + " does not specify any serviceName and the WSDL " + wsdlUrl + " contains " + found
                + ". Set the serviceName attribute of the @WebService annotation on " + sei
                + " to the name of the service you want to use.");
    }
}
