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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;

import dev.galasa.BeforeClass;
import dev.galasa.Test;
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
    public String 		runName;
	
	@Logger
	public Log       	logger;
	
	@HttpClient
    public IHttpClient 	client;
	
	private String 		prefix;
	private String 		testProjectName;
	private String 		managerProjectName;
	
	private Path 		simplatformParentDir;
	private Path 		gradleBin;
	
	@TestProperty(prefix = "gradle.zip",suffix = "location", required = true)
    public String 		gradleZipLocation;
	
	@TestProperty(prefix = "gradle.zip",suffix = "version", required = true)
    public String 		gradleZipVersion;
	
	@BeforeClass
	public void setupTest() throws ResourceUnavailableException, IOException, LinuxManagerException, IpNetworkManagerException {
		prefix = "dev.galasa.simbank";
		testProjectName = prefix + ".tests";
		managerProjectName = prefix + ".manager";

		simplatformParentDir = setupSimPlatform();
		
		gradleBin = installGradle();
	}

	
	private Path setupSimPlatform() throws ResourceUnavailableException, IOException, LinuxManagerException {
		logger.info("Downloading simplatform repository archive - main branch.");
		Path simplatformZip = downloadHttp("https://github.com/galasa-dev/simplatform/archive/main.zip");
		logger.info("Unzipping simplatform repository archive");
		Path simplatformDir = unzipArchiveToTemp(simplatformZip);
		logger.info("Set up repository");
		Path simplatformParent = structureSimplatform(simplatformDir);
		
		refactorSimplatform(simplatformParent);
		
		Path remoteDir = getLinuxImage().getHome().resolve(runName + "/simplatform");
		
		logger.info("Upload simplatform to remote image");
		copyDirectory(simplatformParent, remoteDir);

		return remoteDir;
	}
	
	protected void refactorSimplatform(Path simplatformParent) throws IOException {
		renameFiles(simplatformParent);
		changeAllPrefixes(simplatformParent);
	}
	
		
    private Path downloadHttp(String downloadLocation) throws ResourceUnavailableException {

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
    
	
    private Path unzipArchiveToTemp(Path zipFile) throws IOException {
    	logger.trace("Unzipping: " + zipFile.toString());
		Path unzippedDir = Files.createTempDirectory("galasa.test.compilation.unzipped");
		unzippedDir.toFile().deleteOnExit();
		
		unzipSourceToTarget(zipFile, unzippedDir);
		
		return unzippedDir;
        
	}
	
	private void unzipSourceToTarget(Path source, Path target) throws IOException {
		logger.trace("Unzipping: " + source.toString() + " to " + target.toString());
		ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(source));
        ZipEntry entry;
        while ((entry = zipInputStream.getNextEntry()) != null) {
            final Path entryTarget = target.resolve(entry.getName());
            if (entry.isDirectory()) {
                Files.createDirectory(entryTarget);
            } else {
                Files.copy(zipInputStream, entryTarget);
            }
        }
	}
	
	private Path installGradle() throws ResourceUnavailableException, LinuxManagerException, IOException, IpNetworkManagerException {
		// Download Gradle
		logger.info("Installing Gradle");
		logger.trace("Downloading Gradle Zip from: " + gradleZipLocation);
		Path gradleZip = downloadHttp(gradleZipLocation);
		Path runLocation = getLinuxImage().getHome().resolve(runName);
		Path remoteGradleZip = runLocation.resolve("gradle-" + gradleZipVersion + ".zip");
		Path remoteGradleDir = runLocation.resolve("gradle");
		
		// Upload Gradle
		logger.trace("Uploading gradle archive to remote image");
		logger.trace("Copying: " + gradleZip.toString() + " to " + remoteGradleZip.toString());
		Files.copy(gradleZip, remoteGradleZip);
		
        // Unzip Gradle
		logger.trace("Unzipping gradle archive on remote image");
		String unzipRC = getLinuxImage().getCommandShell().issueCommand(
				"unzip -q " + remoteGradleZip.toString()
				+ " -d " + remoteGradleDir.toString()
				+ "; echo $?"
		);
		
		logger.trace("Checking return code of unzip");
		assertThat(unzipRC).isEqualToIgnoringWhitespace("0");
		
		Path gradleWorkingDir = remoteGradleDir.resolve("gradle-" + gradleZipVersion + "/bin");
		
		logger.trace("Checking unzipped gradle version");
		String gradleVersion = getLinuxImage().getCommandShell().issueCommand(
				gradleWorkingDir + "/gradle -v"
		);
		
		assertThat(gradleVersion).contains(gradleZipVersion);
		
		return gradleWorkingDir;
	}
	
	private Path structureSimplatform(Path unzippedDir) throws IOException {
		// Create new (temp) directory
		logger.trace("Creating project parent directory (for manager and tests)");
		Path parentDir = Files.createTempDirectory("galasa.test.simplatform.parent");
		parentDir.toFile().deleteOnExit();
				
		// Create parent settings file
		
		logger.trace("Creating settings.gradle");
		Path parentSettingsFile = parentDir.resolve("settings.gradle");
		Files.createFile(parentSettingsFile);
		StringBuilder settingsSB = new StringBuilder();
		settingsSB.append("include '");
		settingsSB.append(managerProjectName);
		settingsSB.append("'\n");
		settingsSB.append("include '");
		settingsSB.append(testProjectName);
		settingsSB.append("'\n");
		Files.write(parentSettingsFile, settingsSB.toString().getBytes());
		
		// Get Manager Files
		logger.trace("Copying managers source into parent directory");
		copyDirectory(unzippedDir.resolve("simplatform-main/galasa-simbank-tests/" + managerProjectName + "/"), parentDir.resolve(managerProjectName));
		// Get Tests
		logger.trace("Copying tests source into parent directory");
		copyDirectory(unzippedDir.resolve("simplatform-main/galasa-simbank-tests/" + testProjectName + "/"), parentDir.resolve(testProjectName));

		return parentDir;
	}
	
	private void copyDirectory(Path source, Path target) throws IOException {
		logger.info("Copying Directory: " + source.toString() + " to " + target.toString());		Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
			@Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                Files.createDirectories(target.resolve(source.relativize(dir).toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Files.copy(file, target.resolve(source.relativize(file).toString()));
                return FileVisitResult.CONTINUE;
            }
		});
	}
	
	protected void renameFiles(Path simplatformParent) throws IOException {
		logger.trace("Renaming example files");
		Path managerDir = simplatformParent.resolve(managerProjectName);
		Path testDir = simplatformParent.resolve(testProjectName);
		
        // Manager
		Files.move(managerDir.resolve("settings-example.gradle"), managerDir.resolve("settings.gradle"));		
		Files.move(managerDir.resolve("build-example.gradle"), managerDir.resolve("build.gradle"));
		Files.move(managerDir.resolve("bnd-example.bnd"), managerDir.resolve("bnd.bnd"));
		
        // Tests
		Files.move(testDir.resolve("settings-example.gradle"), testDir.resolve("settings.gradle"));		
		Files.move(testDir.resolve("build-example.gradle"), testDir.resolve("build.gradle"));
		Files.move(testDir.resolve("bnd-example.bnd"), testDir.resolve("bnd.bnd"));
		
	}
	
	
	protected void changeAllPrefixes(Path simplatformParent) throws IOException {
        // Manager
		changePrefix(simplatformParent.resolve(managerProjectName + "/build.gradle"));
		changePrefix(simplatformParent.resolve(managerProjectName + "/settings.gradle"));
        // Tests
		changePrefix(simplatformParent.resolve(testProjectName + "/build.gradle"));
		changePrefix(simplatformParent.resolve(testProjectName + "/settings.gradle"));
	}
	
	
	private void changePrefix(Path file) throws IOException {
		String incumbent = "%%prefix%%";
		String fileData = new String(Files.readAllBytes(file), Charset.defaultCharset());
    	fileData = fileData.replace(incumbent, prefix);
    	Files.write(file, fileData.getBytes());
    	logger.trace("Changing prefix (" + incumbent + ") to \"" + prefix + "\" in file: " + file.toString());
	}
	
	@Test
    public void compile() throws Exception {
		logger.info("Compilation Test");
				
		String javaHomeCommand = "export JAVA_HOME=" + getJavaInstallation().getJavaHome();
			
		logger.info("Running Gradle Build");
		
		String buildCommand = javaHomeCommand + "; "
				// Go to project directory
				+ "cd " + simplatformParentDir.toString() + "; "
				// Build using Gradle
				+ gradleBin.toString() + "/gradle "
				+ "-Dgradle.user.home=" + getLinuxImage().getHome().resolve(runName) + "/.gradle"
				+ "--info "
				+ "build";
		
		logger.info("Issuing Command:");
		logger.info(buildCommand);
		
		String managerBuildResults = getLinuxImage().getCommandShell().issueCommand(buildCommand);
		
		assertThat(managerBuildResults).contains("BUILD SUCCESSFUL");
	}

    abstract protected IGenericEcosystem getEcosystem();

    abstract protected ILinuxImage getLinuxImage();

    abstract protected IJavaUbuntuInstallation getJavaInstallation();

}
