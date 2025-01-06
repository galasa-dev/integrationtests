/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.inttests;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;

import dev.galasa.AfterClass;
import dev.galasa.BeforeClass;
import dev.galasa.Test;
import dev.galasa.TestVariation;
import dev.galasa.TestVariationProperty;
import dev.galasa.artifact.ArtifactManager;
import dev.galasa.artifact.IArtifactManager;
import dev.galasa.artifact.IBundleResources;
import dev.galasa.artifact.ISkeletonProcessor.SkeletonType;
import dev.galasa.ipnetwork.ICommandShell;
import dev.galasa.linux.ILinuxImage;
import dev.galasa.linux.LinuxImage;
import dev.galasa.core.manager.Logger;
import dev.galasa.core.manager.StoredArtifactRoot;
import dev.galasa.core.manager.TestProperty;
import dev.galasa.framework.spi.teststructure.TestStructure;
import dev.galasa.framework.spi.utils.GalasaGson;

/**
 * This integration test will prove that the basic framework is working by
 * running a few easy IVTs.
 * 
 * The test will download the runtime.zip and extract the galasa-boot.jar file.
 * 
 * It will then run the IVTs from the command line.
 * 
 *  
 *
 */
@Test
@TestVariation(name = "openjdk8", defaultVariation = true, properties = {
        @TestVariationProperty(property = "linux.tag.primary.capabilities", value = "java8,maven") })
@TestVariation(name = "openjdk11", properties = {
        @TestVariationProperty(property = "linux.tag.primary.capabilities", value = "java11,maven") })
@TestVariation(name = "ibmjdk8", properties = {
        @TestVariationProperty(property = "linux.tag.primary.capabilities", value = "ibmjava8,maven") })
@TestVariation(name = "ibmjdk11", properties = {
        @TestVariationProperty(property = "linux.tag.primary.capabilities", value = "ibmjava11,maven") })
public class RunCommandlineTests {

    private final Pattern runNamePattern = Pattern.compile("\\QAllocated Run Name \\E(\\w+)\\Q to this run\\E");
    private final GalasaGson    gson           = new GalasaGson();

    @Logger
    public Log            logger;

    @LinuxImage(imageTag = "primary")
    public ILinuxImage    linuxPrimary;

    @StoredArtifactRoot
    public Path           storedArtifactRoot;

    // TODO provide direct access in CPS so we can ignore this suffix/prefix
    // nonsense when it is a fixed property
    @TestProperty(prefix = "integrated.tests", suffix = "maven.repository")
    public String           mavenRepository; // The maven repository that contains the code we will be testing

    @ArtifactManager
    public IArtifactManager artifactManager; // TODO we should get the bundleresources object direct

    private ICommandShell   shell;           // get a command shell
    private Path            homePath;        // The home directory of the default userid

    /**
     * Set up the shell and the filesystem we will use later
     * 
     * @throws Exception - standard catchall
     */
    @BeforeClass
    public void setupShells() throws Exception {
        // *** Obtain the shell that we are going to use
        shell = linuxPrimary.getCommandShell();
        logger.info("Obtained command shell to linux server");

        // *** Obtain the home directory
        this.homePath = linuxPrimary.getHome();
    }

    /**
     * Set up the .m2/settings.xml file read for mvn commands. The repository is
     * provided as a test property
     * 
     * @throws Exception - standard catchall
     */
    @BeforeClass
    public void setupM2() throws Exception {
        // *** Create the .m2 directory if necessary
        Path settings = this.homePath.resolve(".m2/settings.xml");
        if (!Files.exists(settings.getParent())) {
            Files.createDirectory(settings.getParent());
        }

        // TODO should have this as an annotated file
        IBundleResources bundleResources = artifactManager.getBundleResources(getClass());

        // Get the skeleton settings.xml and provide the test repo
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("galasarepo", this.mavenRepository);
        InputStream is = bundleResources.retrieveSkeletonFile("settings.xml", parameters, SkeletonType.VELOCITY);

        // *** Copy the file to the test system
        Files.copy(is, settings);
    }

    /**
     * Retreive the runtime.zip from maven and extract the galasa-boot.jar file
     * 
     * @throws Exception - standard catchall
     */
    @BeforeClass
    public void setupGalasaBoot() throws Exception {
        // *** Retrieve the runtime zip from the maven repository
        String response = this.shell.issueCommand(
                "mvn -B org.apache.maven.plugins:maven-dependency-plugin:2.8:get -Dartifact=dev.galasa:runtime:0.3.0-SNAPSHOT:zip > mvn.log;echo maven-rc=$?");
        assertThat(response).as("maven rc search").contains("maven-rc=0"); // check we exited 0
        Path log = this.homePath.resolve("mvn.log"); // the log file
        Path saLog = this.storedArtifactRoot.resolve("mvn.log"); // stored artifact file
        Files.copy(log, saLog); // copy it

        this.logger.info("Runtime successfully download");

        // *** Unzip the runtime to get the galasa-boot
        response = this.shell.issueCommand(
                "unzip -o .m2/repository/dev/galasa/runtime/0.3.0-SNAPSHOT/runtime-0.3.0-SNAPSHOT.zip > unzip.log;echo zip-rc=$?");
        assertThat(response).as("zip rc search").contains("zip-rc=0"); // check we exited 0
        log = this.homePath.resolve("unzip.log"); // the log file
        saLog = this.storedArtifactRoot.resolve("unzip.log"); // the stored artifact
        Files.copy(log, saLog); // copy it

        this.logger.info("galasa-boot unzipped");
    }

    /**
     * Run the CoreIVT
     * 
     * @throws Exception - standard catchall
     */
    @Test
    public void runCoreIVT() throws Exception {

        // Build the command line we need to run the core ivt
        StringBuilder sb = new StringBuilder();
        sb.append("java "); // Run with the default java installation
        sb.append("-jar galasa-boot.jar "); // The installed boot jar
        sb.append("--remotemaven ");
        sb.append(mavenRepository); // The framework/test maven repository
        sb.append(" ");
        sb.append("--obr mvn:dev.galasa/dev.galasa.uber.obr/0.3.0-SNAPSHOT/obr "); // the framework obr
        sb.append("--obr mvn:dev.galasa/dev.galasa.ivt.obr/0.3.0-SNAPSHOT/obr "); // The IVT Obr
        sb.append("--test dev.galasa.ivt.core/dev.galasa.ivt.core.CoreManagerIVT "); // The Core IVT
        sb.append("--trace "); // Lets get as much bask as we can in case of failure
        sb.append("> coreivt.log "); // Save the log
        sb.append(";echo galasa-boot-rc=$?"); // check that the run ended with exit code 0

        logger.info("About to issue the command :-\n" + sb.toString());

        Instant start = Instant.now();
        String response = shell.issueCommand(sb.toString()); // run the command
        Instant end = Instant.now();
        logger.info("Command returned - took " + (end.getEpochSecond() - start.getEpochSecond()) + " seconds to run");

        Path log = this.homePath.resolve("coreivt.log"); // the log file from the command
        Path runLog = this.storedArtifactRoot.resolve("coreivt.log"); // the stored artifact
        Files.copy(log, runLog); // copy to stored artifacts

        assertThat(response).as("run command").contains("galasa-boot-rc=0"); // check we exited 0

        assertThat(response).as("check there were no warnings issued").doesNotContain("WARNING"); // make sure java
                                                                                                  // didnt issue
                                                                                                  // warnings;

        // *** Pull the run log so we can extract the run name
        String sLog = new String(Files.readAllBytes(log));
        Matcher matcher = runNamePattern.matcher(sLog);
        assertThat(matcher.find()).as("Finding run name in log").isTrue(); // Check that the run name is in the log
        String runName = matcher.group(1);

        logger.info("The CoreIVT test was run name " + runName);

        // *** Retrieve the Test Structure
        Path structureFile = this.homePath.resolve(".galasa/ras/" + runName + "/structure.json");
        assertThat(Files.exists(structureFile)).as("Test structure exists on test server for this run").isTrue(); // Check
                                                                                                                  // that
                                                                                                                  // the
                                                                                                                  // test
                                                                                                                  // structure
                                                                                                                  // exists
        String sStructure = new String(Files.readAllBytes(structureFile));

        TestStructure testStructure = gson.fromJson(sStructure, TestStructure.class);

        // *** Check the test passed
        assertThat(testStructure.getResult()).as("The test structure indicates the test passes").isEqualTo("Passed");

    }

    /**
     * Retrieve the galasa directory including the RAS
     * 
     * @throws Exception
     */
    @AfterClass
    public void getLogs() throws Exception {
        String response = this.shell.issueCommand("zip -r -9 galasa.zip .galasa;echo zip-rc=$?");
        assertThat(response).as("zip rc check is 0").contains("zip-rc=0"); // check we exited 0

        Path zip = this.homePath.resolve("galasa.zip"); // the zip file
        Path sazip = this.storedArtifactRoot.resolve("galasa.zip"); // stored artifact file
        Files.copy(zip, sazip); // copy it

    }

}
