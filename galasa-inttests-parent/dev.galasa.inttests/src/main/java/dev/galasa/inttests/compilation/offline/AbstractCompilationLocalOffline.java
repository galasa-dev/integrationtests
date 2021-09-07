/*
* Copyright contributors to the Galasa project 
*/
package dev.galasa.inttests.compilation.offline;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import dev.galasa.artifact.BundleResources;
import dev.galasa.artifact.IBundleResources;
import dev.galasa.artifact.TestBundleResourceException;
import dev.galasa.framework.spi.ResourceUnavailableException;
import dev.galasa.galasaecosystem.ILocalEcosystem;
import dev.galasa.inttests.compilation.AbstractCompilationLocal;
import dev.galasa.ipnetwork.IpNetworkManagerException;
import dev.galasa.linux.LinuxManagerException;

public abstract class AbstractCompilationLocalOffline extends AbstractCompilationLocal {
       
    @BundleResources
    public IBundleResources resources;
    
    @Override
    protected void setProjectDirectory() throws ResourceUnavailableException, LinuxManagerException, IpNetworkManagerException, IOException, TestBundleResourceException {
        projectDirectory = setupExampleProject();
    }

    private Path setupExampleProject() throws IOException, TestBundleResourceException {
        logger.trace("Setting up example project");
        Path projectDir = testRunDirectory.resolve("helloworld");
        
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("PARENT_FOLDER_NAME", "helloworld");
        parameters.put("PACKAGENAME", "dev.galasa.compilation.test.helloworld");
        parameters.put("DEPENDENCIES", getProjectDependencies());
        parameters.put("REPO", ((ILocalEcosystem) getEcosystem()).getIsolatedDirectory() + "/maven");
        
        Path settingsGradle = projectDir.resolve("settings.gradle");
        Path buildGradle = projectDir.resolve("build.gradle");
        Path javaSrcDir = projectDir.resolve("src/main/java/dev/galasa/compilation/test/helloworld");
        Path javaSrcFile = javaSrcDir.resolve("HelloWorld.java");
        
        logger.trace("Creating files and directories: ");
        logger.trace("Creating directory: " + projectDir.toString());
        Files.createDirectories(projectDir);
        logger.trace("Creating file: " + settingsGradle.toString());
        Files.createFile(settingsGradle);
        logger.trace("Creating file: " + buildGradle.toString());
        Files.createFile(buildGradle);
        logger.trace("Creating directory: " + javaSrcDir.toString());
        Files.createDirectories(javaSrcDir);
        logger.trace("Creating file: " + javaSrcFile.toString());
        Files.createFile(javaSrcFile);
        
        logger.trace("Writing data from example files to project directory");
        Files.write(settingsGradle,
                resources.retrieveSkeletonFileAsString("offlinebuild/exampleSettings.gradle", parameters).getBytes());
        Files.write(buildGradle, 
                resources.retrieveSkeletonFileAsString("offlinebuild/exampleBuild.gradle", parameters).getBytes());
        Files.write(javaSrcFile, 
                resources.retrieveSkeletonFileAsString("offlinebuild/HelloWorld.java", parameters).getBytes());
        
        storeOutput("project-files", settingsGradle);
        storeOutput("project-files", buildGradle);
        storeOutput("project-files", javaSrcFile);
        
        return projectDir;
    }
    
    private String getProjectDependencies() throws TestBundleResourceException, IOException {
        // Iterate dependencies
        String[] managers = getManagers();
        StringBuilder sb = new StringBuilder();

        logger.trace("Adding dependencies to build.gradle:");
        for (int i = 0; i < managers.length; i++) {
            if (managers[i].equals("dev.galasa.selenium.manager")) {
                
                sb.append("    implementation('dev.galasa:dev.galasa.selenium.manager:0.+'){\n" + 
                        "        exclude group: 'com.squareup.okio', module: 'okio'\n" + 
                        "        exclude group: 'com.squareup.okhttp3', module: 'okhttp'\n" + 
                        "        exclude group: 'net.bytebuddy', module: 'byte-buddy'\n" + 
                        "        exclude group: 'org.apache.commons', module: 'commons-exec'\n" + 
                        "        exclude group: 'com.google.guava', module: 'guava'\n" + 
                        "    }\n");
            } else {
                sb.append("    implementation 'dev.galasa:" + managers[i] + ":0.+'\n");
            }
            logger.trace("Dependency: " + managers[i]);
        }
        logger.trace("Adding constraints to build.gradle:");
        sb.append(
            "    constraints {\n" + 
            "        implementation('commons-codec:commons-codec:1.15'){\n" + 
            "            because \"Force specific version of commons-codec for security reasons\"\n" + 
            "        }\n" + 
            "        implementation('org.apache.httpcomponents:httpcore:4.4.14'){\n" + 
            "            because \"Force specific version of httpcore for security reasons\"\n" + 
            "        }\n" + 
            "    }\n");
        return sb.toString();
    }
    
    abstract protected String[] getManagers() throws TestBundleResourceException, IOException;
    
}
