/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2021.
 */
package dev.galasa.inttests.compilation.simbank;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.galasa.galasaecosystem.ILocalEcosystem;

public abstract class AbstractCompilationLocalSimBankOffline extends AbstractCompilationLocalSimBank {
    
    @Override
    protected void refactorSimplatform(Path simplatformParent) throws IOException {
        renameFiles(simplatformParent);
        changeAllPrefixes(simplatformParent);
        
        Path managerBuildGradle = simplatformParent.resolve("dev.galasa.simbank.manager/build.gradle");
        Path testBuildGradle = simplatformParent.resolve("dev.galasa.simbank.tests/build.gradle");
        Path parentSettings = simplatformParent.resolve("settings.gradle");
        
        // Alter project parent
        addPluginManagementRepo(parentSettings);
        
        // Alter manager project
        updateMavenRepo(managerBuildGradle); 
        addDependencyConstraints(managerBuildGradle);
        
        // Alter test project
        updateMavenRepo(testBuildGradle);
        // Add a list of managers to the test(s)
        addDependencyConstraints(testBuildGradle);
        addImplementationConstraints(testBuildGradle);
    }
    
    /*
     * For use when changing source code to work with the isolated zip (either mvp or full).
     * Specify a file to work against. This method will take the contents of that file, and 
     * replace occurrences of mavenCentral() with the appropriate local maven repository closure.
     * 
     * @param fileToChange the path to the file that needs updating
     */
    protected void updateMavenRepo(Path fileToChange) throws IOException {
        String fileData = new String(Files.readAllBytes(fileToChange), Charset.defaultCharset());
        logger.info("Replacing occurences of mavenCentral() with a link to the unzipped archive in file: "
                + fileToChange.getName(fileToChange.getNameCount()-2) + "/" + fileToChange.getFileName());
        fileData = fileData.replace("mavenCentral()",
                "maven {\n" +
                "        url=\"file://" + ((ILocalEcosystem) getEcosystem()).getIsolatedDirectory() + "/maven\"\n" + 
                "    }"
                );
        Files.write(fileToChange, fileData.getBytes());
    }
    
    /*
     * For use when changing source code to work with the isolated zip (either mvp or full).
     * Specify a file to work against. This method will take insert a pluginManagement closure at 
     * the beginning of that file. 
     * 
     * @param gradleSettingsFile the path to the file that needs updating
     */
    protected void addPluginManagementRepo (Path gradleSettingsFile) throws IOException {
        logger.info("Adding pluginManagement closure to: " 
                + gradleSettingsFile.getName(gradleSettingsFile.getNameCount()-2) + "/" + gradleSettingsFile.getFileName());
        
        String fileData = new String(Files.readAllBytes(gradleSettingsFile), Charset.defaultCharset());
        String pluginClosure = "pluginManagement {\n" + 
            "    repositories {\n" + 
            "        maven {\n" + 
            "            url=\"file://" + ((ILocalEcosystem) getEcosystem()).getIsolatedDirectory() + "/maven\"\n" + 
            "        }\n" + 
            "    }\n" + 
            "}\n\n";
        Files.write(gradleSettingsFile, (pluginClosure.concat(fileData)).getBytes());
    }
    
    /*
     * For use when changing source code to work with the isolated zip (either mvp or full).
     * Specify a file to work against. This method will take insert a constraints closure within 
     * the dependencies closure of that file. 
     * 
     * NOTE: enforces explicit versions of commons-coden and httpcore
     * 
     * @param fileToChange the path to the file that needs updating
     */
    protected void addDependencyConstraints(Path fileToChange) throws IOException {
        logger.info("Adding constraints (for http packages) to: " 
                + fileToChange.getName(fileToChange.getNameCount()-2) + "/" + fileToChange.getFileName());
        
        String fileData = new String(Files.readAllBytes(fileToChange), Charset.defaultCharset());
        String constraints = 
            "    constraints {\n" + 
            "        implementation('commons-codec:commons-codec:1.15'){\n" + 
            "            because \"Force specific version of commons-codec for security reasons\"\n" + 
            "        }\n" + 
            "        implementation('org.apache.httpcomponents:httpcore:4.4.14'){\n" + 
            "            because \"Force specific version of httpcore for security reasons\"\n" + 
            "        }\n" + 
            "    }\n";
        
        // Regex Matches:
        // Match 1: The dependencies closure, as well as whatever is inside it, up until just before the final, closing, curly brace.
        // Match 2: The final, closing, curly brace.
        String regex = "(dependencies \\{[\\n\\r\\sa-zA-Z0-9\\'\\.\\:\\+\\-\\(\\)]+)(\\})";
        Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(fileData);
        

		if(!matcher.find()) {
			throw new IOException("Match not found: " + regex + " => " + fileData);
		}
        // Insert the constraints closure between match 1 (dependencies) and match 2 (closing brace)
        fileData = fileData.replace(matcher.group(0), matcher.group(1) + constraints + matcher.group(2));
        
        Files.write(fileToChange, fileData.getBytes());
    }
    
    /*
     * For use when changing source code to work with the isolated zip (either mvp or full).
     * Specify a file to work against. This method will alter the selenium manager depedency
     * to exclude several unnecessary packages that aren't available in the zip.
     * 
     * @param fileToChange the path to the file that needs updating
     */
    protected void addImplementationConstraints(Path fileToChange) throws IOException {
        logger.info("Adding constraints (for selenium manager) to: " 
                + fileToChange.getName(fileToChange.getNameCount()-2) + "/" + fileToChange.getFileName());
        
        String fileData = new String(Files.readAllBytes(fileToChange), Charset.defaultCharset());
          
        String constraints = 
            "    implementation('dev.galasa:dev.galasa.selenium.manager:0.+'){\n" + 
            "        exclude group: 'com.squareup.okio', module: 'okio'\n" + 
            "        exclude group: 'com.squareup.okhttp3', module: 'okhttp'\n" + 
            "        exclude group: 'net.bytebuddy', module: 'byte-buddy'\n" + 
            "        exclude group: 'org.apache.commons', module: 'commons-exec'\n" + 
            "        exclude group: 'com.google.guava', module: 'guava'\n" + 
            "    }";
        
        // Regex Matches:
        // Match 1: The dependencies closure, as well as whatever is inside it, up until just before the final, closing, curly brace.
        // Match 2: The final, closing, curly brace.
        String regex = "implementation\\s\\'dev\\.galasa\\:dev\\.galasa\\.selenium\\.manager\\:0\\.\\+\\'";
        Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(fileData);
        matcher.find();
        // Insert the constraints closure between match 1 (dependencies) and match 2 (closing brace)
        fileData = fileData.replace(matcher.group(0), constraints);
        
        Files.write(fileToChange, fileData.getBytes());
    }

}
