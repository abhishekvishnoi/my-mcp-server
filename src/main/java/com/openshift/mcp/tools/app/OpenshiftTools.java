package com.openshift.mcp.tools.app;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.stream.Collectors;

public class OpenshiftTools {

    @Inject
    OpenShiftClient openShiftClient;


    @Tool(description = "Create an new application in a project.")
    String createApplicationInOpenshift(@ToolArg(description = "The name of the project in the openShift cluster " +
            " (e.g. kafka , openshift-pipelines)") String projectName ,
                              @ToolArg(description = "The name of the image to be used for the pod") String imageName ,
                              @ToolArg(description = "The name of the application to be created") String appName ,
                              @ToolArg(description = "The name of the container port " ,defaultValue = "8080") Integer containerPort ) {


        Map<String, String> labels = Map.of("app", appName);

        Deployment deployment = new DeploymentBuilder()
                .withNewMetadata()
                    .withName(appName)
                    .withLabels(labels)
                .endMetadata()
                .withNewSpec()
                    .withReplicas(1)
                    .withNewSelector()
                    .withMatchLabels(labels)
                    .endSelector()
                .withNewTemplate()
                    .withNewMetadata()
                        .withLabels(labels)
                    .endMetadata()
                    .withNewSpec()
                        .addNewContainer()
                            .withName(appName)
                            .withImage(imageName)
                            .addNewPort()
                                .withContainerPort((containerPort))
                            .endPort()
                        .endContainer()
                    .endSpec()
                .endTemplate()
                .endSpec()
                .build();

        openShiftClient.apps().deployments()
                .inNamespace(projectName)
                .resource(deployment)
                .create();

        // 2) Service
        Service service = new ServiceBuilder()
                .withNewMetadata()
                .withName(appName)
                .withLabels(labels)
                .endMetadata()
                .withNewSpec()
                .withSelector(labels)
                .addNewPort()
                .withName("http")
                .withPort((containerPort))
                .withTargetPort(new IntOrString(containerPort))
                .endPort()
                .endSpec()
                .build();

        openShiftClient.services()
                .inNamespace(projectName)
                .resource(service)
                .create();

        // 3) Route (OpenShift-only)
        Route route = new RouteBuilder()
                .withNewMetadata()
                .withName(appName)
                .addToLabels("app", "quarkus-app")
                .endMetadata()
                .withNewSpec()
                .withNewTo()
                .withKind("Service")
                .withName(appName) // Name of the target service
                .endTo()
                .withNewPort()
                .withNewTargetPort(containerPort) // e.g., 8080 or "http"
                .endPort()
                // Optional: .withHost("://domain.com")
                .endSpec()
                .build();


        openShiftClient.routes()
                .inNamespace(projectName)
                .resource(route)
                .create();


        String host = route.getSpec() != null ? route.getSpec().getHost() : null;

        return "Created app '" + appName + "' in '" + projectName + "'"
                + " (Deployment + Service + Route)"
                + (host != null ? "; URL: http://" + host : "");



    }
    
}
