package io.quarkiverse.cxf.deployment.test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;

import javax.jws.WebParam;
import javax.jws.WebService;

/**
 * Class Documentation
 *
 * <p>
 * What is the point of this class?
 *
 * @author geronimo1
 */

@WebService(endpointInterface = "io.quarkiverse.cxf.deployment.test.FruitWebService", serviceName = "FruitWebService")
public class ExoticFruitWebServiceImpl implements FruitWebService {

    private Set<Fruit> fruits = Collections.newSetFromMap(Collections.synchronizedMap(new LinkedHashMap<>()));

    public ExoticFruitWebServiceImpl() {
        fruits.add(new Fruit("Chayote", "Native Mexico"));
        fruits.add(new Fruit("Starfruit", "United States"));
    }

    @Override
    public int count() {
        return (fruits != null ? fruits.size() : 0);
    }

    @Override
    public void add(@WebParam(name = "fruit") Fruit fruit) {
        fruits.add(fruit);
    }

    @Override
    public void delete(@WebParam(name = "fruit") Fruit fruit) {
        fruits.remove(fruit);
    }
}
