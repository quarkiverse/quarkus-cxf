package io.quarkiverse.cxf.deployment.test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;

import jakarta.jws.WebParam;
import jakarta.jws.WebService;

@WebService(endpointInterface = "io.quarkiverse.cxf.deployment.test.FruitWebService", serviceName = "FruitWebService")
public class FruitWebServiceImpl implements FruitWebService {

    private Set<Fruit> fruits = Collections.newSetFromMap(Collections.synchronizedMap(new LinkedHashMap<>()));

    public FruitWebServiceImpl() {
        fruits.add(new Fruit("Apple", "Winter fruit"));
        fruits.add(new Fruit("Banana", "Summer fruit"));
    }

    @Override
    public int count() {
        return (fruits != null ? fruits.size() : 0);
    }

    @Override
    public Set<Fruit> add(@WebParam(name = "fruit") Fruit fruit) {
        fruits.add(fruit);
        return fruits;
    }

    @Override
    public void delete(@WebParam(name = "fruit") Fruit fruit) {
        fruits.remove(fruit);
    }

    @Override
    public String getDescriptionByName(String name) {
        return fruits.stream()
                .filter(f -> name.equals(f.getName()))
                .map(Fruit::getDescription)
                .findFirst()
                .orElse(null);
    }
}
