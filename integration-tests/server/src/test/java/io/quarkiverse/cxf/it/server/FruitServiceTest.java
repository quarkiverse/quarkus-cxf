package io.quarkiverse.cxf.it.server;

import java.util.LinkedHashSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class FruitServiceTest {

    @Test
    public void crud() {
        final LinkedHashSet<Fruit> expectedFruits = new LinkedHashSet<>();
        expectedFruits.add(new Fruit("Apple", "Winter fruit"));
        expectedFruits.add(new Fruit("Pineapple", "Tropical fruit"));
        final FruitService client = QuarkusCxfClientTestUtil.getClient(FruitService.class, "/soap/fruits");
        Assertions.assertEquals(expectedFruits, client.list());

        final Fruit orange = new Fruit("Orange", "Mediterranean fruit");
        expectedFruits.add(orange);
        Assertions.assertEquals(expectedFruits, client.add(orange));

        expectedFruits.remove(orange);
        Assertions.assertEquals(expectedFruits, client.delete(orange));
    }

}
