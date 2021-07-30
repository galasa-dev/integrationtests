/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2021.
 */
package dev.galasa.inttests.compilation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;

import dev.galasa.BeforeClass;
import dev.galasa.Test;
import dev.galasa.artifact.TestBundleResourceException;
import dev.galasa.core.manager.Logger;
import dev.galasa.core.manager.RunName;
import dev.galasa.core.manager.TestProperty;
import dev.galasa.framework.spi.ResourceUnavailableException;
import dev.galasa.galasaecosystem.IGenericEcosystem;
import dev.galasa.http.HttpClient;
import dev.galasa.http.IHttpClient;
import dev.galasa.ipnetwork.IpNetworkManagerException;
import dev.galasa.java.ubuntu.IJavaUbuntuInstallation;
import dev.galasa.linux.ILinuxImage;
import dev.galasa.linux.LinuxManagerException;

public abstract class AbstractCompilationLocal {
    
    @RunName
    public String           runName;
    
    @Logger
    public Log              logger;
    
    @HttpClient
    public IHttpClient      client;
    
    protected Path          testRunDirectory;
    protected Path          projectDirectory;
    private Path            gradleBin;
    
    @TestProperty(prefix = "gradle.zip",suffix = "location", required = true)
    public String           gradleZipLocation;
    
    public String           gradleZipVersion;
    
    protected String        javaHomeCommand;
    
    /*
     * Initialise all resources needed for testing.
     * 
     */
    @BeforeClass
    public void setupTest() throws ResourceUnavailableException, IOException, LinuxManagerException, IpNetworkManagerException, TestBundleResourceException {
        javaHomeCommand = "export JAVA_HOME=" + getJavaInstallation().getJavaHome();
        
        testRunDirectory = getLinuxImage().getHome().resolve(runName);
        
        setProjectDirectory();
        
        gradleZipVersion = getGradleVersion();
        gradleBin = installGradle();
    }
    
    
    /*
     * Extracts the version from the gradle zip path
     */
    private String getGradleVersion() {
        final String regex = "gradle-(\\d+\\.\\d+\\.\\d+)-bin\\.zip";

        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(gradleZipLocation);
        matcher.find();
        gradleZipVersion = matcher.group(1);
        
        return gradleZipVersion;
    }

    /*
     * Method for unpacking an archive on a remote image.
     * 
     * @param   archive     Path to the remote archive
     * @param   target      Path to the directory to which archive should be unpacked 
     */
    protected void unpackOnRemote(Path archive, Path target) throws IpNetworkManagerException, LinuxManagerException {
        logger.trace("Unzipping archive \"" + archive.toString() + "\" to \"" + target.toString() + "\"");
        String unpackRC = getLinuxImage().getCommandShell().issueCommand(
                "unzip -q " + archive.toString()
                + " -d " + target.toString()
                + "; echo RC=$?"
        );
        
        logger.trace("Checking return code of unpack");
        assertThat(unpackRC).isEqualToIgnoringWhitespace("RC=0");
    }
        
    /*
     * Method for downloading a resource from a http location.
     * 
     * @param   downloadLocation    string containing the url of the resource to be downloaded
     * @return  tempFile            the temporary location of the file that was downloaded.
     */
    protected Path downloadHttp(String downloadLocation) throws ResourceUnavailableException {

        logger.trace("Retrieving Http Resource: " + downloadLocation);

        URI uri;
        try {
            uri = new URI(downloadLocation);
        } catch (URISyntaxException e) {
            throw new ResourceUnavailableException("Invalid Download Location: " + downloadLocation, e);
        }
        
        client.setURI(uri);

        try (CloseableHttpResponse response = client.getFile(uri.getPath())) {

            Path archive = Files.createTempFile("galasa.test.compilation", ".temp");
            archive.toFile().deleteOnExit();

            HttpEntity entity = response.getEntity();

            Files.copy(entity.getContent(), archive, StandardCopyOption.REPLACE_EXISTING);

            return archive;
        } catch (ConnectionClosedException e) {
            logger.error("Transfer connection closed early, usually caused by network instability, marking as resource unavailable so can try again later",e);
            throw new ResourceUnavailableException("Network error downloading from: " + uri.toString(), e);
        } catch (Exception e) {
            throw new ResourceUnavailableException("Unable to download from: " + uri.toString(), e);
        }
    }
        
    /*
     * Installs gradle (Download locally, upload to image, and unzip on image), 
     * then returns the path to the gradle bin directory. 
     * 
     * @return    gradleWorkingDir    The path to the gradle installation directory.
     */
    private Path installGradle() throws ResourceUnavailableException, LinuxManagerException, IOException, IpNetworkManagerException {
        // Download Gradle
        logger.info("Installing Gradle");
        logger.trace("Downloading Gradle Zip from: " + gradleZipLocation);
        Path localGradleArchive = downloadHttp(gradleZipLocation);
        Path runLocation = getLinuxImage().getHome().resolve(runName);
        Path remoteGradleArchive = runLocation.resolve("gradle-" + gradleZipVersion + ".zip");
        Path remoteGradleDir = runLocation.resolve("gradle");
        
        // Upload Gradle
        logger.trace("Uploading gradle archive to remote image");
        logger.trace("Copying: " + localGradleArchive.toString() + " to " + remoteGradleArchive.toString());
        Files.copy(localGradleArchive, remoteGradleArchive);
        
        // Unzip Gradle
        logger.trace("Unzipping gradle archive on remote image");
        unpackOnRemote(remoteGradleArchive, remoteGradleDir);
        
        Path gradleWorkingDir = remoteGradleDir.resolve("gradle-" + gradleZipVersion + "/bin");
        
        logger.trace("Checking unpacked gradle version");
        String gradleVersion = getLinuxImage().getCommandShell().issueCommand(
                javaHomeCommand + "; " +
                gradleWorkingDir + "/gradle -v"
        );
        
        assertThat(gradleVersion).contains(gradleZipVersion);
        
        return gradleWorkingDir;
    }
        
    
    /*
     * Runs and Gradle build against the prepared Simplatform code.
     * Passes if "BUILD SUCCESSFUL" appears in the output.
     * 
     */
    @Test
    public void compile() throws Exception {
        logger.info("Compilation Test");
        
        logger.info("Running Gradle Build");
        
        // Set Java Home, go to project directory, execute the unpackaged Gradle binary.
        // Pass to the Gradle binary: 
        //         * User home directory
        //         * Option to ensure output is logger friendly
        //        * The task(s) to be executed.
        String buildCommand = javaHomeCommand + "; "
                + "cd " + projectDirectory.toString() + "; "
                + gradleBin.toString() + "/gradle "
                + "-Dgradle.user.home=" + testRunDirectory + "/.gradle "
                + "--console plain "
                + "build";
        
        logger.info("Issuing Command: " + buildCommand);
        
        String managerBuildResults = getLinuxImage().getCommandShell().issueCommand(buildCommand);
        
        assertThat(managerBuildResults).contains("BUILD SUCCESSFUL");
        logger.info("OUTPUT FOR TEST: " + managerBuildResults);
    }
    
    /*
     * Sets the project directory to run the gradle build against
     * 
     */
    abstract protected void setProjectDirectory() throws ResourceUnavailableException, LinuxManagerException, IpNetworkManagerException, IOException, TestBundleResourceException;

    
    /*
     * @return  ecosystem   The ecosystem instance associated with the test.
     * 
     */
    abstract protected IGenericEcosystem getEcosystem();
    
    /*
     * @return  linuxImage  The linux image instance associated with the test.
     * 
     */
    abstract protected ILinuxImage getLinuxImage();

    /*
     * @return  javaInstallation    The java installation instance associated with the test.
     * 
     */
    abstract protected IJavaUbuntuInstallation getJavaInstallation();

}
