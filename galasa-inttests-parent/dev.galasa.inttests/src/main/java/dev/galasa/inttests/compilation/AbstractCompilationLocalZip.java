package dev.galasa.inttests.compilation;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.galasa.galasaecosystem.ILocalEcosystem;

public abstract class AbstractCompilationLocalZip extends AbstractCompilationLocal {
	
	protected String[] mvpManagers = {
		"dev.galasa.core.manager",
		"dev.galasa.artifact.manager",
		"dev.galasa.http.manager",
		"dev.galasa.docker.manager",
		"dev.galasa.cicsts.ceci.manager",
		"dev.galasa.zos3270.manager"
	};
	
	protected String[] allManagers = {
		"dev.galasa.windows.manager",
		"dev.galasa.zosunixcommand.ssh.manager",
		"dev.galasa.zos3270.manager",
		"dev.galasa.zosfile.rseapi.manager",
		"dev.galasa.zosbatch.rseapi.manager",
		"dev.galasa.zosrseapi.manager",
		"dev.galasa.zos.manager",
		"dev.galasa.zosliberty.manager",
		"dev.galasa.zostsocommand.ssh.manager",
		"dev.galasa.zosmf.manager",
		"dev.galasa.zosconsole.zosmf.manager",
		"dev.galasa.zosprogram.manager",
		"dev.galasa.zosbatch.zosmf.manager",
		"dev.galasa.zosfile.zosmf.manager",
		"dev.galasa.zosconsole.oeconsol.manager",
		"dev.galasa.jmeter.manager",
		"dev.galasa.selenium.manager",
		"dev.galasa.java.ubuntu.manager",
		"dev.galasa.java.windows.manager",
		"dev.galasa.java.manager",
		"dev.galasa.textscan.manager",
		"dev.galasa.core.manager",
		"dev.galasa.artifact.manager",
		"dev.galasa.galasaecosystem.manager",
		"dev.galasa.phoenix2.manager",
		"dev.galasa.elasticlog.manager",
		"dev.galasa.ipnetwork.manager",
		"dev.galasa.http.manager",
		"dev.galasa.liberty.manager",
		"dev.galasa.kubernetes.manager",
		"dev.galasa.openstack.manager",
		"dev.galasa.docker.manager",
		"dev.galasa.cicsts.ceda.manager",
		"dev.galasa.cicsts.manager",
		"dev.galasa.cicsts.ceci.manager",
		"dev.galasa.cicsts.resource.manager",
		"dev.galasa.cicsts.cemt.manager",
		"dev.galasa.linux.manager",
		"dev.galasa.artifact.manager",
		"dev.galasa.http.manager",
		"dev.galasa.artifact.manager",
		"dev.galasa.http.manager"
	};
	
	/*
     * Within the specified file, this method replaces occurrences of mavenCentral() with the 
     * appropriate local maven repository closure.
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
     * Inserts pluginManagement closure at the start of the specified file.
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
     * Adds a "constraints" closure in the file specified.
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
    	matcher.find();
    	// Insert the constraints closure between match 1 (dependencies) and match 2 (closing brace)
    	fileData = fileData.replace(matcher.group(0), matcher.group(1) + constraints + matcher.group(2));
    	
    	Files.write(fileToChange, fileData.getBytes());
    }
       
    /*
     * Change selenium manager to require less, in the file specified.
     */
    protected void addImplementationConstraints(Path fileToChange) throws IOException {
    	logger.info("Adding constraints (for selenium manager) to: " 
    			+ fileToChange.getName(fileToChange.getNameCount()-2) + "/" + fileToChange.getFileName());
    	
    	String fileData = new String(Files.readAllBytes(fileToChange), Charset.defaultCharset());
	  	
    	String constraints = 
    		"    implementation('dev.galasa:dev.galasa.selenium.manager:0.+'){\n" + 
    		"		exclude group: 'com.squareup.okio', module: 'okio'\n" + 
    		"		exclude group: 'com.squareup.okhttp3', module: 'okhttp'\n" + 
    		"		exclude group: 'net.bytebuddy', module: 'byte-buddy'\n" + 
    		"		exclude group: 'org.apache.commons', module: 'commons-exec'\n" + 
    		"		exclude group: 'com.google.guava', module: 'guava'\n" + 
    		"	}";
    	
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
    
	protected void addManagerDependencies(Path fileToChange, String[] dependencies) throws IOException {
		logger.info("Adding managers (as dependencies) to: " 
    			+ fileToChange.getName(fileToChange.getNameCount()-2) + "/" + fileToChange.getFileName());
		String fileData = new String(Files.readAllBytes(fileToChange), Charset.defaultCharset());
	  	
    	// Regex Matches:
    	// Match 1: The dependencies closure, as well as whatever is inside it, up until just before the final, closing, curly brace.
    	// Match 2: The final, closing, curly brace.
    	String regex = "(dependencies \\{[\\n\\r\\sa-zA-Z0-9\\'\\.\\:\\+\\-\\(\\)]+)(\\})";
    	Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
    	Matcher matcher = pattern.matcher(fileData);
    	matcher.find();
    	
    	String incumbentDependencyClosure = matcher.group(1);
    	String postClosure = matcher.group(2);
    	
    	// Iterate dependencies
    	StringBuilder sb = new StringBuilder();
    	for (int i = 0; i < dependencies.length; i++) {
    		if (!incumbentDependencyClosure.contains(dependencies[i])) {
    			sb.append("\timplementation 'dev.galasa:" + dependencies[i] + ":0.+'\n");
    		}
        }
    	
    	// Insert the dependencies between match 1 (dependencies) and match 2 (closing brace)
    	fileData = fileData.replace(matcher.group(0), incumbentDependencyClosure.concat(sb.toString()).concat(postClosure));
    	
    	Files.write(fileToChange, fileData.getBytes());
		
	}

}
