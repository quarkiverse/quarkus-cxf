package io.quarkiverse.cxf.devconsole;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.spi.CDI;

import io.quarkiverse.cxf.CXFClientInfo;

public class DevCxfClientInfosSupplier implements Supplier<Collection<CXFClientInfo>> {

    @Override
    public List<CXFClientInfo> get() {
        List<CXFClientInfo> clientInfos = new ArrayList<>(allClientInfos());
        clientInfos.sort(Comparator.comparing(CXFClientInfo::getSei));
        return clientInfos;
    }

    public static Collection<CXFClientInfo> allClientInfos() {
        return CDI.current().select(CXFClientInfo.class).stream().collect(Collectors.toCollection(ArrayList::new));
    }
}
