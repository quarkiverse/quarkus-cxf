package io.quarkiverse.cxf.it.clustering;

import java.util.Arrays;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.cxf.clustering.LoadDistributorFeature;
import org.apache.cxf.clustering.SequentialStrategy;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class LoadDistributorFeatureBean extends LoadDistributorFeature {

    @Inject
    public LoadDistributorFeatureBean(
            @ConfigProperty(name = "cxf.it.calculator1.baseUri") String calculator1BaseUri,
            @ConfigProperty(name = "cxf.it.calculator2.baseUri") String calculator2BaseUri) {
        SequentialStrategy sequentialStrategy = new SequentialStrategy();
        sequentialStrategy.setAlternateAddresses(
                Arrays.asList(
                        calculator1BaseUri + "/calculator-ws/CalculatorService",
                        calculator2BaseUri + "/calculator-ws/CalculatorService"));

        setStrategy(sequentialStrategy);
    }
}
