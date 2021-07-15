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
	
	@BeforeClass
	public void setupTest() throws ResourceUnavailableException, IOException {
		prefix = "dev.galasa.simbank";
		
		Path simDir = setupSimPlatform();
		
		logger.info("Simplatform Path:" + simDir.toString());
	}

	
	private Path setupSimPlatform() throws ResourceUnavailableException, IOException {
		Path simplatformZip = downloadHttp("https://github.com/galasa-dev/simplatform/archive/main.zip");
		Path simplatformDir = unzipArchive(simplatformZip);
		
		Path simplatformParent = structureSimplatform(simplatformDir);

		return simplatformParent; // Eventually return remote dir with correct file(s)
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
		String testProjectName = prefix + ".tests";
		String managerProjectName = prefix + ".manager";
		
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
                Files.createDirectories(target.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)));
                return FileVisitResult.CONTINUE;
            }
		});
	}
	
	@Test
    public void compile() throws Exception {
		logger.info("Noddy Test");
	}

    abstract protected IGenericEcosystem getEcosystem();

    abstract protected ILinuxImage getLinuxImage();

    abstract protected IJavaUbuntuInstallation getJavaInstallation();

}
