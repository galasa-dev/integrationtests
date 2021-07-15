/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2021.
 */
package dev.galasa.inttests.compilation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
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
	
	@BeforeClass
	public void setupTest() throws ResourceUnavailableException, IOException {
		Path simDir = setupSimPlatform();
		
		logger.info("Simplatform Path:" + simDir.toString());
	}
	
	private Path setupSimPlatform() throws ResourceUnavailableException, IOException {
		Path simplatformZip = downloadHttp("https://github.com/galasa-dev/simplatform/archive/main.zip");
		Path simplatformDir = unzipArchive(simplatformZip);

		return simplatformDir;
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
            logger.info("Entry name = " + entry.getName());
            final Path entryTarget = target.resolve(entry.getName());
            if (entry.isDirectory()) {
                Files.createDirectory(entryTarget);
            } else {
                Files.copy(zipInputStream, entryTarget);
            }
        }
	}
	
	
	@Test
    public void compile() throws Exception {
		logger.info("Noddy Test");
	}

    abstract protected IGenericEcosystem getEcosystem();

    abstract protected ILinuxImage getLinuxImage();

    abstract protected IJavaUbuntuInstallation getJavaInstallation();

}
