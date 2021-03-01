package io.quarkiverse.cxf.tracing;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import io.opentracing.propagation.TextMap;

public class HeaderExtractAdapter implements TextMap {

    private final Map<String, List<String>> headers;

    public HeaderExtractAdapter(Map<String, List<String>> headers) {
        this.headers = headers;
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        return new MultivaluedMapFlatIterator<>(this.headers.entrySet());
    }

    @Override
    public void put(String key, String value) {
        throw new UnsupportedOperationException("This class should be used only with Tracer.inject()!");
    }

    protected Map<String, List<String>> servletHeadersToMultiMap(HttpServletRequest httpServletRequest) {
        final Map<String, List<String>> headersResult = new HashMap<>();

        final Enumeration<String> headerNamesIt = httpServletRequest.getHeaderNames();
        while (headerNamesIt.hasMoreElements()) {
            final String headerName = headerNamesIt.nextElement();

            final Enumeration<String> valuesIt = httpServletRequest.getHeaders(headerName);
            final List<String> valuesList = new ArrayList<>(1);
            while (valuesIt.hasMoreElements()) {
                valuesList.add(valuesIt.nextElement());
            }

            headersResult.put(headerName, valuesList);
        }

        return headersResult;
    }

    public static final class MultivaluedMapFlatIterator<K, V> implements Iterator<Map.Entry<K, V>> {

        private final Iterator<Map.Entry<K, List<V>>> mapIterator;
        private Map.Entry<K, List<V>> mapEntry;
        private Iterator<V> listIterator;

        public MultivaluedMapFlatIterator(Set<Map.Entry<K, List<V>>> multiValuesEntrySet) {
            this.mapIterator = multiValuesEntrySet.iterator();
        }

        @Override
        public boolean hasNext() {
            if (this.listIterator != null && this.listIterator.hasNext()) {
                return true;
            }

            return this.mapIterator.hasNext();
        }

        @Override
        public Map.Entry<K, V> next() {
            if (this.mapEntry == null || (!this.listIterator.hasNext() && this.mapIterator.hasNext())) {
                this.mapEntry = this.mapIterator.next();
                this.listIterator = this.mapEntry.getValue().iterator();
            }

            if (this.listIterator.hasNext()) {
                return new AbstractMap.SimpleImmutableEntry<>(this.mapEntry.getKey(), this.listIterator.next());
            } else {
                return new AbstractMap.SimpleImmutableEntry<>(this.mapEntry.getKey(), null);
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
