package io.quarkiverse.cxf.it.server;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.jws.WebService;

@WebService(serviceName = "FruitService")
public class FruitServiceImpl implements FruitService {

    private Set<Fruit> fruits = Collections.synchronizedSet(new LinkedHashSet<>());

    public FruitServiceImpl() {
        fruits.add(new Fruit("Apple", "Winter fruit"));
        fruits.add(new Fruit("Pineapple", "Tropical fruit"));
    }

    @Override
    public Set<Fruit> list() {
        return fruits;
    }

    @Override
    public Set<Fruit> add(Fruit fruit) {
        fruits.add(fruit);
        return fruits;
    }

    @Override
    public Set<Fruit> delete(Fruit fruit) {
        fruits.remove(fruit);
        return fruits;
    }
}