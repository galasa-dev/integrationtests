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
	
	/*
	 * TODO
	 * [x] Download Simplatform
	 * [x] Unzip Simplatform
	 * [x] Move Simplatform files & code to working directory
	 * [x] Managers - Change Prefix
	 * [x] Tests - Change Prefix
	 * Zip:
	 * 	   [ ] Managers - Add PluginManagement Closure
	 *     [ ] Managers - Change Repositories 
	 *     [ ] Tests - Add PluginManagement Closure
	 *     [ ] Tests - Change Repositories
	 *     Isolated: 
	 *         [ ] Add all managers to test dependencies
	 * Test:
	 * [x] Build Manager
	 * [x] Build Tests
	 */
	
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
		Path simplatformZip = downloadHttp("https://github.com/galasa-dev/simplatform/archive/main.zip");
		Path simplatformDir = unzipArchiveToTemp(simplatformZip);
		
		Path simplatformParent = structureSimplatform(simplatformDir);
		
		makeChanges(simplatformParent);
		
		Path remoteDir = getLinuxImage().getHome().resolve(runName + "/simplatform");
		
		// Upload to linux image
		copyDirectory(simplatformParent, remoteDir);

		return remoteDir;
	}
	
	protected void makeChanges(Path simplatformParent) throws IOException {
		renameFiles(simplatformParent);
		changeAllPrefixes(simplatformParent);
		logger.info("Calling NON-Overridden Method");
	}
	
		
    private Path downloadHttp(String downloadLocation) throws ResourceUnavailableException {

        logger.trace("Retrieving Http Resource: " + downloadLocation);

        URI uri;
        try {
            uri = new URI(downloadLocation);
        } catch (URISyntaxException e) {
            throw new ResourceUnavailableException("Invalid Download Location", e);
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
            throw new ResourceUnavailableException("Unable to download from: " + downloadLocation, e);
        }
    }
    
	
    private Path unzipArchiveToTemp(Path zipFile) throws IOException {
		Path unzippedDir = Files.createTempDirectory("galasa.test.compilation.unzipped");
		unzippedDir.toFile().deleteOnExit();
		
		unzipSourceToTarget(zipFile, unzippedDir);
		
		return unzippedDir;
        
	}
	
	private void unzipSourceToTarget(Path source, Path target) throws IOException {
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
		Path gradleZip = downloadHttp(gradleZipLocation);
		Path runLocation = getLinuxImage().getHome().resolve(runName);
		Path remoteGradleZip = runLocation.resolve("gradle-" + gradleZipVersion + ".zip");
		Path remoteGradleDir = runLocation.resolve("gradle");
		
		// Upload Gradle
		Files.copy(gradleZip, remoteGradleZip);
		
		// Unzip Gradle
		getLinuxImage().getCommandShell().issueCommand(
				"unzip " + remoteGradleZip.toString()
				+ " -d " + remoteGradleDir.toString()
		);
		
		Path gradleWorkingDir = remoteGradleDir.resolve("gradle-" + gradleZipVersion + "/bin");
		
		return gradleWorkingDir;
	}
	
	private Path structureSimplatform(Path unzippedDir) throws IOException {
		// Create new (temp) directory
		Path parentDir = Files.createTempDirectory("galasa.test.simplatform.parent");
		parentDir.toFile().deleteOnExit();
				
		// Create parent settings file
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
		copyDirectory(unzippedDir.resolve("simplatform-main/galasa-simbank-tests/" + managerProjectName + "/"), parentDir.resolve(managerProjectName));
		// Get Tests
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
		logger.info("Renaming example files");
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
		
		String fileData = new String(Files.readAllBytes(file), Charset.defaultCharset());
    	fileData = fileData.replace("%%prefix%%", prefix);
    	Files.write(file, fileData.getBytes());
    	logger.info("Prefix in " + file.toString() + " changed.");
	}

	
	@Test
    public void compile() throws Exception {
		logger.info("Compilation Test");
		assertThat(true).isTrue();
		
		String javaHomeCommand = "export JAVA_HOME=" + getJavaInstallation().getJavaHome();
		
		logger.info("Running Gradle Build");
		String managerBuildResults = getLinuxImage().getCommandShell().issueCommand(
			javaHomeCommand + "; "
			// Go to project directory
			+ "cd " + simplatformParentDir.toString() + "; "
			// Build using Gradle
			+ gradleBin.toString() + "/gradle "
			+ "-Dgradle.user.home=" + getLinuxImage().getHome().resolve(runName) + "/.gradle"
			+ "--info "
			+ "build"
		);
		assertThat(managerBuildResults).contains("BUILD SUCCESSFUL");
		logger.info(managerBuildResults);
	}

    abstract protected IGenericEcosystem getEcosystem();

    abstract protected ILinuxImage getLinuxImage();

    abstract protected IJavaUbuntuInstallation getJavaInstallation();

}
