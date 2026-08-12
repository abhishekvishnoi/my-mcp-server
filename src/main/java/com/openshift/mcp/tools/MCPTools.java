package com.openshift.mcp.tools;


import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.openshift.api.model.ProjectList;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class MCPTools {

    @Inject
    OpenShiftClient openShiftClient;

    @Tool(description = "List all active projects/namespaces in the OpenShift cluster")
    public String listProjects() {
        try {
            ProjectList list = openShiftClient.projects().list();
            if (list == null || list.getItems().isEmpty()) {
                return "No projects found or insufficient permissions.";
            }
            return list.getItems().stream()
                    .map(proj -> String.format("- %s (Status: %s)",
                            proj.getMetadata().getName(),
                            proj.getStatus().getPhase()))
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            return "Failed to list projects: " + e.getMessage();
        }
    }

    @Tool(description = "List all pods running in a specific project/namespace")
    public String listPods(
            @ToolArg(description = "The name of the project/namespace") String project) {
        try {
            PodList podList = openShiftClient.pods().inNamespace(project).list();
            if (podList == null || podList.getItems().isEmpty()) {
                return "No pods found in project: " + project;
            }
            return podList.getItems().stream()
                    .map(pod -> String.format("- %s [Phase: %s, PodIP: %s]",
                            pod.getMetadata().getName(),
                            pod.getStatus().getPhase(),
                            pod.getStatus().getPodIP()))
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            return "Failed to list pods in project " + project + ": " + e.getMessage();
        }
    }

    @Tool(description = "Create a test server pod in a specific project/namespace")
    public String createTestPod(
            @ToolArg(description = "The name of the target project") String project,
            @ToolArg(description = "The unique name of the pod") String podName,
            @ToolArg(description = "The container image to deploy (e.g., 'nginx' or 'quay.io/quarkus/quarkus-dist-openshift')") String image) {
        try {
            Pod pod = new PodBuilder()
                    .withNewMetadata()
                    .withName(podName)
                    .addToLabels("app", "openshift-mcp-test")
                    .endMetadata()
                    .withNewSpec()
                    .addNewContainer()
                    .withName("main-container")
                    .withImage(image)
                    .addNewPort()
                    .withContainerPort(80)
                    .endPort()
                    .endContainer()
                    .endSpec()
                    .build();

            Pod created = openShiftClient.pods().inNamespace(project).resource(pod).create();
            return String.format("Pod '%s' successfully spawned in project '%s'. Status: %s",
                    created.getMetadata().getName(),
                    project,
                    created.getStatus() != null ? created.getStatus().getPhase() : "Pending");
        } catch (Exception e) {
            return "Failed to create pod: " + e.getMessage();
        }
    }
}