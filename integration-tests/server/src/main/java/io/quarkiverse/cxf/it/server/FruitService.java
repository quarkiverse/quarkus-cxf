package io.quarkiverse.cxf.it.server;

import java.util.Set;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

@WebService
public interface FruitService {

    @WebMethod
    Set<Fruit> list();

    @WebMethod
    Set<Fruit> add(Fruit fruit);

    @WebMethod
    Set<Fruit> delete(Fruit fruit);
}