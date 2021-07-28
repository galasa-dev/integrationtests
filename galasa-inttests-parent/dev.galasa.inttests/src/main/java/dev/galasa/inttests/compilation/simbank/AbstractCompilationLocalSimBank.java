/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2021.
 */
package dev.galasa.inttests.compilation.simbank;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import dev.galasa.framework.spi.ResourceUnavailableException;
import dev.galasa.inttests.compilation.AbstractCompilationLocal;
import dev.galasa.ipnetwork.IpNetworkManagerException;
import dev.galasa.linux.LinuxManagerException;

public abstract class AbstractCompilationLocalSimBank extends AbstractCompilationLocal {
    
    private String          prefix = "dev.galasa.simbank";
    private String          testProjectName = prefix + ".tests";
    private String          managerProjectName = prefix + ".manager";
    
    @Override
    protected void setProjectDirectory() throws ResourceUnavailableException, LinuxManagerException, IpNetworkManagerException, IOException {
    	projectDirectory = setupSimPlatform();
    }
    
    /*
     * Download simplatform (github main branch), unzipp simplatform, then structure 
     * and edit the directory and files it in such a way that they are usable by gradle.
     * 
     * @return  remoteDirectory     the parent directory of the structured, edited, and usable simplatform project.
     * 
     */
    private Path setupSimPlatform() throws ResourceUnavailableException, IOException, LinuxManagerException, IpNetworkManagerException {
        logger.info("Downloading simplatform repository archive - main branch.");
        Path localArchive = downloadHttp("https://github.com/galasa-dev/simplatform/archive/main.zip");
        
        logger.info("Uploading simplatform repository archive to image");
        Path remoteArchive = testRunDirectory.resolve("simplatformArchive");
        Path remoteUnpacked = testRunDirectory.resolve("simplatformUnpacked");
        Files.copy(localArchive, remoteArchive);
        
        logger.info("Unzipping simplatform repository archive");
        unpackOnRemote(remoteArchive, remoteUnpacked);
        
        logger.info("Setting up repository");
        Path simplatformParent = testRunDirectory.resolve("simplatform");
        Files.createDirectories(simplatformParent);
        structureSimplatform(remoteUnpacked, simplatformParent);
        refactorSimplatform(simplatformParent);
        
        return simplatformParent;
    }
    
    /*
     * Parent method for renaming of files and changing of prefixes.
     * To be overridden by subclasses to make further changes (e.g. building from isolated zip requires more changes)  
     * 
     * @param   simplatformParent   the path to the parent directory of the simplatform project
     */
    protected void refactorSimplatform(Path simplatformParent) throws IOException {
        renameFiles(simplatformParent);
        changeAllPrefixes(simplatformParent);
    }
    
    /*
     * Retrieves the necessary source files from the unpacked simplatform archive and 
     * structures them in a parent directory, along with a parent settings file.
     * 
     * @param   unpackedDir     The directory containing he unpackaged simplatform archive  
     * @return  parentDir       The parent directory of the newly structured simplatform parent project
     * 
     */
    private void structureSimplatform(Path unpackedDir, Path simplatformParent) throws IOException, IpNetworkManagerException, LinuxManagerException {
        // Create new (temp) directory
        logger.trace("Creating project parent directory (for manager and tests)");
        
        // Create parent settings file
        
        logger.trace("Creating settings.gradle");
        Path parentSettingsFile = simplatformParent.resolve("settings.gradle");
//        Files.createFile(parentSettingsFile);
        
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
        moveFilesOnRemote(
                unpackedDir.resolve("simplatform-main/galasa-simbank-tests/" + managerProjectName),
                simplatformParent.resolve(managerProjectName)
            );
        
        // Get Tests
        logger.trace("Copying tests source into parent directory");
        moveFilesOnRemote(
                unpackedDir.resolve("simplatform-main/galasa-simbank-tests/" + testProjectName), 
                simplatformParent.resolve(testProjectName)
            );
        
    }
    
    /*
     * Moves file (source) to target.
     * 
     * @param	source	Path: The file to be moved
     * @param	target	Path: The location to be moved to
     */
    private void moveFilesOnRemote(Path source, Path target) throws IpNetworkManagerException, LinuxManagerException {
        String command = "mv " + source.toString() + " " + target.toString() + "; echo RC=$?";
        logger.info("issuing command: " + command);
    	String rc = getLinuxImage().getCommandShell().issueCommand(command);
        assertThat(rc).isEqualToIgnoringWhitespace("RC=0");
    }
    
    /*
     * Renames the example files with names that Gradle will recognise.
     * 
     * @param   simplatformParent   The directory of the Simplatform project
     */
    protected void renameFiles(Path simplatformParent) throws IOException {
        logger.trace("Renaming example files");
        Path managerDir = simplatformParent.resolve(managerProjectName);
        Path testDir = simplatformParent.resolve(testProjectName);
        
        // Managers
        Files.move(managerDir.resolve("settings-example.gradle"), managerDir.resolve("settings.gradle"));        
        Files.move(managerDir.resolve("build-example.gradle"), managerDir.resolve("build.gradle"));
        Files.move(managerDir.resolve("bnd-example.bnd"), managerDir.resolve("bnd.bnd"));
        
        // Tests
        Files.move(testDir.resolve("settings-example.gradle"), testDir.resolve("settings.gradle"));        
        Files.move(testDir.resolve("build-example.gradle"), testDir.resolve("build.gradle"));
        Files.move(testDir.resolve("bnd-example.bnd"), testDir.resolve("bnd.bnd"));
        
    }
    
    /*
     * Renames the prefix placeholders within the build and settings files of the 
     * manager and test projects.
     * 
     * @param   simplatformParent   The directory of the simplatform project
     */
    protected void changeAllPrefixes(Path simplatformParent) throws IOException {
        // Manager
        changePrefix(simplatformParent.resolve(managerProjectName + "/build.gradle"));
        changePrefix(simplatformParent.resolve(managerProjectName + "/settings.gradle"));
        // Tests
        changePrefix(simplatformParent.resolve(testProjectName + "/build.gradle"));
        changePrefix(simplatformParent.resolve(testProjectName + "/settings.gradle"));
    }
    
    /*
     * Renames the prefix placeholders within the specified file
     * 
     * @param   file   The file in which the prefix will be changed.
     */
    private void changePrefix(Path file) throws IOException {
        String incumbent = "%%prefix%%";
        String fileData = new String(Files.readAllBytes(file), Charset.defaultCharset());
        fileData = fileData.replace(incumbent, prefix);
        Files.write(file, fileData.getBytes());
        logger.trace("Changing prefix (" + incumbent + ") to \"" + prefix + "\" in file: " + file.toString());
    }
    
}
