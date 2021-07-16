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
import dev.galasa.framework.spi.ResourceUnavailableException;
import dev.galasa.galasaecosystem.IGenericEcosystem;
import dev.galasa.http.HttpClient;
import dev.galasa.http.IHttpClient;
import dev.galasa.java.ubuntu.IJavaUbuntuInstallation;
import dev.galasa.linux.ILinuxImage;
import dev.galasa.linux.LinuxManagerException;

public abstract class AbstractCompilationLocal {
	
	/*
	 * TODO
	 * [x] Download Simplatform
	 * [x] Unzip Simplatform
	 * [ ] Move Simplatform files & code to working directory
	 * [ ] Managers - Change Prefix
	 * [ ] Tests - Change Prefix
	 * Zip:
	 * 	   [ ] Managers - Add PluginManagement Closure
	 *     [ ] Managers - Change Repositories 
	 *     [ ] Tests - Add PluginManagement Closure
	 *     [ ] Tests - Change Repositories
	 *     Isolated: 
	 *         [ ] Add all managers to test dependencies
	 * Test:
	 * [ ] Build Manager
	 * [ ] Build Tests
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
	
	private Path simplatformParentDir;
	
	@BeforeClass
	public void setupTest() throws ResourceUnavailableException, IOException, LinuxManagerException {
		prefix = "dev.galasa.simbank";
		testProjectName = prefix + ".tests";
		managerProjectName = prefix + ".manager";

		
		simplatformParentDir = setupSimPlatform();
		
		logger.info("Simplatform Path:" + simplatformParentDir.toString());
	}

	
	private Path setupSimPlatform() throws ResourceUnavailableException, IOException, LinuxManagerException {
		Path simplatformZip = downloadHttp("https://github.com/galasa-dev/simplatform/archive/main.zip");
		Path simplatformDir = unzipArchive(simplatformZip);
		
		Path simplatformParent = structureSimplatform(simplatformDir);
		
		makeChanges(simplatformParent);
		
		Path remoteDir = getLinuxImage().getHome();
		
		// Upload to linux image
		copyDirectory(simplatformParent, remoteDir.resolve("simplatform"));

		return simplatformParent; // Eventually return remote dir with correct file(s)
	}
	
	private void makeChanges(Path simplatformParent) throws IOException {
		renameFiles(simplatformParent);
		changeAllPrefixes(simplatformParent);
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
    
	private Path unzipArchive(Path zipFile) throws IOException {
		Path unzippedDir = Files.createTempDirectory("galasa.test.compilation.unzipped");
		unzippedDir.toFile().deleteOnExit();
        
		unzipArchiveToTarget(zipFile, unzippedDir);
		
		return unzippedDir;
        
	}
	
	private void unzipArchiveToTarget(Path source, Path target) throws IOException {
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
		Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
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
	
	private void renameFiles(Path simplatformParent) throws IOException {
		Path managerDir = simplatformParent.resolve(managerProjectName);
		Path testDir = simplatformParent.resolve(testProjectName);
		
		Files.move(managerDir.resolve("settings-example.gradle"), managerDir.resolve("settings.gradle"));		
		Files.move(managerDir.resolve("build-example.gradle"), managerDir.resolve("build.gradle"));
		Files.move(managerDir.resolve("bnd-example.bnd"), managerDir.resolve("bnd.bnd"));
		
		Files.move(testDir.resolve("settings-example.gradle"), testDir.resolve("settings.gradle"));		
		Files.move(testDir.resolve("build-example.gradle"), testDir.resolve("build.gradle"));
		Files.move(testDir.resolve("bnd-example.bnd"), testDir.resolve("bnd.bnd"));
		
	}
	
	private void changeAllPrefixes(Path simplatformParent) throws IOException {
		// manager build
		changePrefix(simplatformParent.resolve(managerProjectName + "/build.gradle"));
		// manager settings
		changePrefix(simplatformParent.resolve(managerProjectName + "/settings.gradle"));
		// test build
		changePrefix(simplatformParent.resolve(testProjectName + "/build.gradle"));
		// test settings
		changePrefix(simplatformParent.resolve(testProjectName + "/settings.gradle"));
	}
	
	private void changePrefix(Path file) throws IOException {
		String fileData = new String(Files.readAllBytes(file), Charset.defaultCharset());
    	fileData = fileData.replace("%%prefix%%", prefix);
    	Files.write(file, fileData.getBytes());
	}
	
	@Test
    public void compile() throws Exception {
		logger.info("Noddy Test");
		assertThat(true).isTrue();
	}

    abstract protected IGenericEcosystem getEcosystem();

    abstract protected ILinuxImage getLinuxImage();

    abstract protected IJavaUbuntuInstallation getJavaInstallation();

}
