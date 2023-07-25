/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.inttests;

import org.apache.commons.logging.Log;

import dev.galasa.Test;
import dev.galasa.artifact.BundleResources;
import dev.galasa.artifact.IBundleResources;
import dev.galasa.artifact.TestBundleResourceException;
import dev.galasa.core.manager.Logger;
import dev.galasa.docker.DockerContainer;
import dev.galasa.docker.DockerManagerException;
import dev.galasa.docker.IDockerContainer;
import dev.galasa.docker.IDockerExec;
import static org.assertj.core.api.Assertions.assertThat;

@Test
public class VscodeIntegrationTests {
    @Logger
    public Log logger;

    @DockerContainer(image = "lukasmarivoet/vscode:latest", start = false)
    public IDockerContainer container1;

    @DockerContainer(image = "lukasmarivoet/vscode:latest", start = false)
    public IDockerContainer container2;

    @BundleResources
    public IBundleResources resource;

    @Test
    public void installExtensionFromMarketPlace() throws DockerManagerException, TestBundleResourceException {
        container1.start();

        IDockerExec exec1 = container1.exec(60000, "code", "--user-data-dir", "local", "--install-extension", "galasa.galasa-plugin");
        exec1.waitForExec();
        logger.info(exec1.getCurrentOutput());

        IDockerExec exec2 = container1.exec(60000,"code", "--user-data-dir", "local", "--list-extensions");
        exec2.waitForExec();

        assertThat(exec2.getCurrentOutput()).contains("galasa.galasa-plugin");
        assertThat(exec2.getCurrentOutput()).contains("redhat.java");
        assertThat(exec2.getCurrentOutput()).contains("vscjava.vscode-java-debug");
        assertThat(exec2.getCurrentOutput()).contains("vscjava.vscode-java-dependency");
        assertThat(exec2.getCurrentOutput()).contains("vscjava.vscode-java-pack");
        assertThat(exec2.getCurrentOutput()).contains("vscjava.vscode-java-test");
        assertThat(exec2.getCurrentOutput()).contains("vscjava.vscode-maven");
    
        container1.stop();
    }

    @Test
    public void installExtensionFromVsix() throws DockerManagerException, TestBundleResourceException {
        container2.start();

        container2.storeFile("/tmp/galasa-plugin.vsix", resource.retrieveFile("galasa-plugin.vsix"));

        IDockerExec exec1 = container2.exec(60000, "code", "--user-data-dir", "local", "--install-extension", "/tmp/galasa-plugin.vsix");
        exec1.waitForExec();

        IDockerExec exec2 = container2.exec(60000,"code", "--user-data-dir", "local", "--list-extensions");
        exec2.waitForExec();

        assertThat(exec2.getCurrentOutput()).contains("galasa.galasa-plugin");
        assertThat(exec2.getCurrentOutput()).contains("redhat.java");
        assertThat(exec2.getCurrentOutput()).contains("vscjava.vscode-java-debug");
        assertThat(exec2.getCurrentOutput()).contains("vscjava.vscode-java-dependency");
        assertThat(exec2.getCurrentOutput()).contains("vscjava.vscode-java-pack");
        assertThat(exec2.getCurrentOutput()).contains("vscjava.vscode-java-test");
        assertThat(exec2.getCurrentOutput()).contains("vscjava.vscode-maven");
    
        container2.stop();
    }
}