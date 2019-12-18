/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.inttests;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.logging.Log;

import dev.galasa.BeforeClass;
import dev.galasa.ICredentialsUsernamePassword;
import dev.galasa.Test;
import dev.galasa.artifact.ArtifactManager;
import dev.galasa.artifact.IArtifactManager;
import dev.galasa.ipnetwork.ICommandShell;
import dev.galasa.ipnetwork.IpNetworkManagerException;
import dev.galasa.linux.ILinuxImage;
import dev.galasa.linux.LinuxImage;
import dev.galasa.core.manager.CoreManager;
import dev.galasa.core.manager.ICoreManager;
import dev.galasa.core.manager.Logger;
import dev.galasa.core.manager.StoredArtifactRoot;
import dev.galasa.core.manager.TestProperty;

/**
 * This integration test will prove that Galasa ecosysem is working with the
 * IVTs
 * 
 * Will pull the Docker Images, extract the runtime artifact and use the sample
 * scripts to build the docker ecosystem. It will then run all the appropriate
 * IVTs against it in automation. Checks to see if the automation runs have been
 * fully cleaned up afterwards.
 * 
 * @author Michael Baylis
 *
 */
@Test
public class RunDockerTests {

//	private final Gson    gson = CirilloGsonBuilder.build();

    @Logger
    public Log         logger;

    @LinuxImage(imageTag = "primary", capabilities = { "docker", "maven" })
    public ILinuxImage linuxPrimary;

    @StoredArtifactRoot
    public Path        storedArtifactRoot;

    // TODO provide direct access in CPS so we can ignore this suffix/prefix
    // nonsense when it is a fixed property
    @TestProperty(prefix = "integrated.tests", suffix = "docker.version")
    public String           dockerVersion;   // The version of the docker images we are testing

    @ArtifactManager
    public IArtifactManager artifactManager; // TODO we should get the bundleresources object direct

    @CoreManager
    public ICoreManager     coreManager;

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
     * Clean everything up, just incase rerunning on a DSE system
     * 
     * @throws Exception
     */
    @BeforeClass
    public void precleanDocker() throws Exception {

        logger.info("Deleting any Galasa Docker containers/images/network that could have been left hanging around");

        String response = shell.issueCommand(
                "docker rm -f test-resources galasa-api galasa-resmon galasa-ras galasa-cps galasa-resources galasa-controller galasa-couchdb-init");
        logger.info("delete containers response :-\n" + response);

        response = shell.issueCommand("docker rmi quay.io/coreos/etcd:v3.2.25" + " couchdb:2"
                + " cicsts-docker-local.artifactory.swg-devops.com/galasa-ras-couchdb-init-amd64:" + dockerVersion
                + " cicsts-docker-local.artifactory.swg-devops.com/galasa-api-bootstrap-amd64:" + dockerVersion
                + " cicsts-docker-local.artifactory.swg-devops.com/galasa-resources:" + dockerVersion
                + " cicsts-docker-local.artifactory.swg-devops.com/galasa-boot-embedded:" + dockerVersion);
        logger.info("delete images response :-\n" + response);

        response = shell.issueCommand("docker network rm galasa");
        logger.info("delete network response :-\n" + response);

        response = shell.issueCommand("docker volume rm -f galasa-etcd galasa-couchdb");
        logger.info("delete volumes response :-\n" + response);

        // *** Clear our test directory
        response = shell.issueCommand("rm -rf galasa-test");

        // *** Clear maven
        response = shell.issueCommand("rm -rf .m2/repository/dev/galasa");
    }

    /**
     * Obtain the runtime.zip contents as this contains the scripts for the Galasa
     * Docker Ecosystem
     * 
     * @throws Exception Catch all
     */
    @Test
    public void obtainRuntimeFolder() throws Exception {

        // *** Logon to the docker repository
        logger.info("Login to the docker repository");
        ICredentialsUsernamePassword dockerCreds = (ICredentialsUsernamePassword) coreManager.getCredentials("w3");
        String response = shell
                .issueCommand("docker login -u " + dockerCreds.getUsername() + " -p " + dockerCreds.getPassword()
                        + " cicsts-docker-local.artifactory.swg-devops.com/galasa-resources;echo cmd-rc=$?");
        assertThat(response).as("Logon Docker").contains("cmd-rc=0"); // check we exited 0

        // *** Pull the resources image
        logger.info("Pulling the resources docker image");
        response = shell.issueCommand("docker pull cicsts-docker-local.artifactory.swg-devops.com/galasa-resources:"
                + dockerVersion + ";echo cmd-rc=$?");
        assertThat(response).as("Pull resources image").contains("cmd-rc=0"); // check we exited 0

        // *** run a special container for the purposes of extracting the runtime.zip
        logger.info("Starting a testing resource container");
        response = shell.issueCommand(
                "docker run --name test-resources -d -p 8880:80 cicsts-docker-local.artifactory.swg-devops.com/galasa-resources:"
                        + dockerVersion + ";echo cmd-rc=$?");
        assertThat(response).as("Start Test Resources image").contains("cmd-rc=0"); // check we exited 0

        // *** Create the test folder
        Path testPath = homePath.resolve("galasa-test");
        Files.createDirectory(testPath);

        // *** Fetch the runtime.zip
        logger.info("Fetching the runtime.zip");
        response = shell.issueCommand("mvn org.apache.maven.plugins:maven-dependency-plugin:2.1:get "
                + "-DrepoUrl=http://127.0.0.1:8880/maven "
                + "-Dartifact=dev.galasa:runtime:0.3.0-SNAPSHOT:zip;echo cmd-rc=$?");
        assertThat(response).as("Fetch runtime.zip").contains("cmd-rc=0"); // check we exited 0

        // *** Unzip the runtime.zip
        logger.info("Unzipping runtime.zip");
        response = shell.issueCommand("unzip " + "-d galasa-test "
                + ".m2/repository/dev/galasa/runtime/0.3.0-SNAPSHOT/runtime-0.3.0-SNAPSHOT.zip;echo cmd-rc=$?");
        assertThat(response).as("Unzip runtime.zip").contains("cmd-rc=0"); // check we exited 0

        logger.info("We now have the runtime.zip ready for building the Galasa Ecosystem");
    }

    /**
     * Pull all the images for the ecosystem
     * 
     * @throws Exception catchall
     */
    @Test
    public void pullAllImages() throws Exception {
        logger.info("Pull all the required images");
        String response = shell.issueCommand("bash -e galasa-test/docker/pull.sh;echo cmd-rc=$?", 10 * 60 * 1000);
        assertThat(response).as("Pull ecosystem images").contains("cmd-rc=0"); // check we exited 0
    }

    /**
     * Create the docker network
     * 
     * @throws Exception catchall
     */
    @Test
    public void createNetwork() throws Exception {
        logger.info("Create the Docker Network");
        String response = shell.issueCommand("bash -e galasa-test/docker/network.sh;echo cmd-rc=$?");
        assertThat(response).as("Create the Docker Network").contains("cmd-rc=0"); // check we exited 0
    }

    /**
     * Create the docker volumes
     * 
     * @throws Exception catchall
     */
    @Test
    public void createVolumes() throws Exception {
        logger.info("Create the Docker Volumes");
        String response = shell.issueCommand("bash -e galasa-test/docker/volumes.sh;echo cmd-rc=$?");
        assertThat(response).as("Create the Docker Volumes").contains("cmd-rc=0"); // check we exited 0
    }

    /**
     * Start the Offical Resources Container
     * 
     * @throws Exception catchall
     */
    @Test
    public void startResources() throws Exception {
        logger.info("Start the Offical Resources Container");
        String response = shell.issueCommand("bash -e galasa-test/docker/resources.sh;echo cmd-rc=$?");
        assertThat(response).as("Start the Resources Container").contains("cmd-rc=0"); // check we exited 0

        Instant expire = Instant.now();
        expire = expire.plusSeconds(120);
        boolean started = false;
        while (Instant.now().compareTo(expire) < 0) {
            logger.info("Checking to see if the Resources Container has started");
            response = shell.issueCommand("docker logs galasa-resources");
            if (response.contains("resuming normal operations")) {
                started = true;
                logger.info("Resources Container started");
                break;
            }

            Thread.sleep(1000);
        }
        assertThat(started).as("Resources Container Started").isTrue();
    }

    /**
     * Start the CPS Container
     * 
     * @throws Exception catchall
     */
    @Test
    public void startCps() throws Exception {
        logger.info("Start the CPS Container");
        String response = shell.issueCommand("bash -e galasa-test/docker/cps-etcd.sh;echo cmd-rc=$?");
        assertThat(response).as("Start the CPS Container").contains("cmd-rc=0"); // check we exited 0

        Instant expire = Instant.now();
        expire = expire.plusSeconds(120);
        boolean started = false;
        while (Instant.now().compareTo(expire) < 0) {
            logger.info("Checking to see if the CPS has started");
            response = shell.issueCommand("docker logs galasa-cps");
            if (response.contains("ready to serve client requests")) {
                started = true;
                logger.info("CPS Container started");
                break;
            }

            Thread.sleep(1000);
        }
        assertThat(started).as("CPS Started").isTrue();
    }

    /**
     * Configure the CPS
     * 
     * @throws Exception catchall
     */
    @Test
    public void configureCps() throws Exception {
        logger.info("Setting the CPS configuration");
        String response = shell
                .issueCommand("ETCDCTL_API=3 etcdctl put framework.dynamicstatus.store etcd:http://172.21.0.1:2379");
        assertThat(response).as("Set DSS").contains("OK"); // check we exited 0

        response = shell
                .issueCommand("ETCDCTL_API=3 etcdctl put framework.resultarchive.store couchdb:http://172.21.0.1:5984");
        assertThat(response).as("Set RAS").contains("OK"); // check we exited 0

        response = shell
                .issueCommand("ETCDCTL_API=3 etcdctl put framework.credentials.store etcd:http://172.21.0.1:2379");
        assertThat(response).as("Set CREDS").contains("OK"); // check we exited 0

        response = shell
                .issueCommand("ETCDCTL_API=3 etcdctl put framework.resource.management.dead.heartbeat.timeout 60");
        assertThat(response).as("Set heartbeat timeout").contains("OK"); // check we exited 0

        response = shell.issueCommand("ETCDCTL_API=3 etcdctl put framework.resource.management.finished.timeout 60");
        assertThat(response).as("Set finished timeout").contains("OK"); // check we exited 0
    }

    /**
     * Start the RAS Container
     * 
     * @throws Exception catchall
     */
    @Test
    public void startRas() throws Exception {
        logger.info("Initialise the RAS Couchdb volume");
        String response = shell.issueCommand("bash -e galasa-test/docker/ras-couchdb-init.sh;echo cmd-rc=$?");
        assertThat(response).as("Initialise the RAS Volume").contains("cmd-rc=0"); // check we exited 0

        logger.info("Start the RAS Container");
        response = shell.issueCommand("cd galasa-test/docker;bash -e ras-couchdb.sh;echo cmd-rc=$?");
        assertThat(response).as("Start the RAS Container").contains("cmd-rc=0"); // check we exited 0

        Instant expire = Instant.now();
        expire = expire.plusSeconds(120);
        boolean started = false;
        while (Instant.now().compareTo(expire) < 0) {
            logger.info("Checking to see if the RAS has started");
            response = shell.issueCommand("docker logs galasa-ras");
            if (response.contains("couch_replicator_clustering : cluster stable")) {
                started = true;
                logger.info("RAS Container started");
                break;
            }

            Thread.sleep(1000);
        }
        assertThat(started).as("RAS Started").isTrue();
    }

    /**
     * Start the API Container
     * 
     * @throws Exception catchall
     */
    @Test
    public void startApi() throws Exception {
        logger.info("Start the API Container");
        String response = shell.issueCommand("cd galasa-test/docker;bash -e api.sh;echo cmd-rc=$?");
        assertThat(response).as("Start the API Container").contains("cmd-rc=0"); // check we exited 0

        Instant expire = Instant.now();
        expire = expire.plusSeconds(120);
        boolean started = false;
        while (Instant.now().compareTo(expire) < 0) {
            logger.info("Checking to see if the API has started");
            response = shell.issueCommand("curl http://127.0.0.1:8181/bootstrap"); // See if the bootstrap servlet is
                                                                                   // active
            if (response.contains("framework.config.store=")) {
                started = true;
                logger.info("API Container started");
                break;
            }

            Thread.sleep(1000);
        }
        assertThat(started).as("API Started").isTrue();
    }

    /**
     * Start the ResMon Container
     * 
     * @throws Exception catchall
     */
    @Test
    public void startResMon() throws Exception {
        logger.info("Start the ResMon Container");
        String response = shell.issueCommand("cd galasa-test/docker;bash -e resource-monitor.sh;echo cmd-rc=$?");
        assertThat(response).as("Start the ResMon Container").contains("cmd-rc=0"); // check we exited 0

        Instant expire = Instant.now();
        expire = expire.plusSeconds(120);
        boolean started = false;
        while (Instant.now().compareTo(expire) < 0) {
            logger.info("Checking to see if the ResMon has started");
            response = shell.issueCommand("docker logs galasa-resmon");
            if (response.contains("Resource Manager has started")) {
                started = true;
                logger.info("ResMon Container started");
                break;
            }

            Thread.sleep(1000);
        }
        assertThat(started).as("ResMon Started").isTrue();
    }

    /**
     * Start the Docker Controller Container
     * 
     * @throws Exception catchall
     */
    @Test
    public void startController() throws Exception {
        logger.info("Start the Docker Controller Container");
        String response = shell.issueCommand("cd galasa-test/docker;bash -e controller.sh;echo cmd-rc=$?");
        assertThat(response).as("Start the Docker Controller Container").contains("cmd-rc=0"); // check we exited 0

        Instant expire = Instant.now();
        expire = expire.plusSeconds(120);
        boolean started = false;
        while (Instant.now().compareTo(expire) < 0) {
            logger.info("Checking to see if the Docker Controller has started");
            response = shell.issueCommand("docker logs galasa-controller");
            if (response.contains("Docker controller has started")) {
                started = true;
                logger.info("Docker Controller Container started");
                break;
            }

            Thread.sleep(1000);
        }
        assertThat(started).as("Docker Controller Started").isTrue();
    }

    /**
     * Run the CoreIVT in automation - check this before running any other test
     * 
     * @throws Exception - standard catchall
     */
    @Test
    public void runCoreIVT() throws Exception {

        submitTest("dev.galasa.ivt.core", "dev.galasa.ivt.core.CoreManagerIVT", "CORE1");

        HashMap<String, String> rasIds = new HashMap<>();

        Instant expire = Instant.now();
        expire = expire.plusSeconds(120);
        boolean started = false;
        while (Instant.now().compareTo(expire) < 0) {
            logger.info("Checking to see if the CoreIVT has finished");
            if (hasRunFinished("CORE1", rasIds)) {
                started = true;
                logger.info("CoreIVT finished");
                break;
            }

            Thread.sleep(1000);
        }
        assertThat(started).as("CoreIVT Finished").isTrue();

        // TODO Check if passed and retrieve the run log
        // TODO Cant do that until the couchdb ras records the id in the dss
        // TODO then use curl to retrieve the data

    }

    /**
     * Run the Remaining IVTs in automation
     * 
     * @throws Exception - standard catchall
     */
    @Test
    public void runAllIVTs() throws Exception {

        submitTest("dev.galasa.ivt.core", "dev.galasa.ivt.core.ArtifactManagerIVT", "ART1");
        submitTest("dev.galasa.ivt.network", "dev.galasa.ivt.network.HttpManagerIVT", "HTTP1");

        HashSet<String> runNames = new HashSet<>();
        runNames.add("ART1");
        runNames.add("HTTP1");

        HashMap<String, String> rasIds = new HashMap<>();

        Instant expire = Instant.now();
        expire = expire.plusSeconds(120);
        boolean finished = false;
        while (Instant.now().compareTo(expire) < 0) {
            logger.info("Checking to see if all the runs have finished");
            Iterator<String> i = runNames.iterator();
            while (i.hasNext()) {
                String runName = i.next();
                if (hasRunFinished(runName, rasIds)) {
                    i.remove();
                }
            }

            if (runNames.isEmpty()) {
                logger.info("All runs finished");
                finished = true;
                break;
            }

            Thread.sleep(1000);
        }
        assertThat(finished).as("All runs finished").isTrue();

        // TODO Check each run passed and retrieve the run log
        // TODO Cant do that until the couchdb ras records the id in the dss
        // TODO then use curl to retrieve the data

    }

    /**
     * Checking to see if the runs are cleaned up after the 60 second finished
     * timeout
     * 
     * @throws Exception - standard catchall
     */
    @Test
    public void checkRunFinishedCleanup() throws Exception {

        logger.info("Waiting 60 seconds for the Run Finished cleanup routines to run");
        Thread.sleep(20000);
        logger.info("40 seconds to go");
        Thread.sleep(20000);
        logger.info("20 seconds to go");
        Thread.sleep(20000);

        Instant expire = Instant.now();
        expire = expire.plusSeconds(120);
        boolean deleted = false;
        while (Instant.now().compareTo(expire) < 0) {
            logger.info("Checking to see if all the runs have been deleted");
            String response = shell.issueCommand("ETCDCTL_API=3 etcdctl get --prefix dss.framework.run.");
            if (response.trim().isEmpty()) {
                deleted = true;
                logger.info("All runs deleted");
                break;
            }

            Thread.sleep(1000);
        }
        assertThat(deleted).as("All runs deleted").isTrue();
    }

    /**
     * Checking to see if the run containers are cleaned up after the runs are
     * deleted
     * 
     * @throws Exception - standard catchall
     */
    @Test
    public void checkRunContainerCleanup() throws Exception {

        HashSet<String> containerNames = new HashSet<>();
        containerNames.add("docker-standard-engine-core1");
        containerNames.add("docker-standard-engine-art1");
        containerNames.add("docker-standard-engine-http1");

        Instant expire = Instant.now();
        expire = expire.plusSeconds(120);
        boolean deleted = false;
        while (Instant.now().compareTo(expire) < 0) {
            logger.info("Checking to see if all the run contianers have been deleted");
            Iterator<String> i = containerNames.iterator();
            while (i.hasNext()) {
                String containerName = i.next();

                String response = shell.issueCommand("docker inspect " + containerName);
                if (response.contains("No such object")) {
                    logger.info("Run Container " + containerName + " deleted");
                    i.remove();
                }
            }

            if (containerNames.isEmpty()) {
                logger.info("All run containers deleted");
                deleted = true;
                break;
            }

            Thread.sleep(1000);
        }
        assertThat(deleted).as("All run containers deleted").isTrue();
    }

    // TODO Further integration testing

    private boolean hasRunFinished(String runName, HashMap<String, String> rasIds) throws IpNetworkManagerException {
        String response = shell.issueCommand("ETCDCTL_API=3 etcdctl get dss.framework.run." + runName + ".status");
        if (!response.contains("finished")) {
            return false;
        }

        logger.info("Run " + runName + " finished");

        response = shell.issueCommand("ETCDCTL_API=3 etcdctl get dss.framework.run." + runName + ".ras.id");
        response = response.trim();
        if (!response.isEmpty()) {
            rasIds.put(runName, response);
        }

        return true;
    }

    private void submitTest(String bundle, String test, String runName) throws IpNetworkManagerException {

        putRunProperty(runName, "status", "creating");
        putRunProperty(runName, "request.type", "inttests");
        putRunProperty(runName, "requestor", "Integrated Tests");
        putRunProperty(runName, "local", "false");
        putRunProperty(runName, "obr", "mvn:dev.galasa/dev.galasa.ivt.obr/0.3.0-SNAPSHOT/obr");
        putRunProperty(runName, "test", bundle + "/" + test);
        putRunProperty(runName, "bundle", bundle);
        putRunProperty(runName, "testclass", test);
        putRunProperty(runName, "repository", "http://172.21.0.1:8080/maven");
        putRunProperty(runName, "queued", Instant.now().toString());

        // *** Now put it on the queue
        putRunProperty(runName, "status", "queued");

    }

    private void putRunProperty(String runName, String property, String value) throws IpNetworkManagerException {
        String response = shell.issueCommand(
                "ETCDCTL_API=3 etcdctl put dss.framework.run." + runName + "." + property + " '" + value + "'");
        assertThat(response).as("PUT was OK").contains("OK"); // check we exited 0
    }

    // TODO retrieve all the RAS and docker logs etc

//	/**
//	 * Retrieve the galasa directory including the RAS
//	 * @throws Exception 
//	 */
//	@AfterClass
//	public void getLogs() throws Exception {
//		String response = this.shell.issueCommand("zip -r -9 galasa.zip .galasa;echo zip-rc=$?");
//		assertThat(response).as("zip rc check is 0").contains("zip-rc=0"); // check we exited 0
//		
//		Path zip = this.homePath.resolve("galasa.zip");  // the zip file
//		Path sazip = this.storedArtifactRoot.resolve("galasa.zip"); // stored artifact file
//		Files.copy(zip, sazip); //copy it
//			
//	}

}
