package com.openshift.mcp;

import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@Singleton
public class OpenShiftClientProducer {

    @Produces
    public OpenShiftClient openshiftClient() {
        // here you would create a custom client
        return new DefaultOpenShiftClient();
    }
}
