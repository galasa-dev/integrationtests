package dev.galasa.inttests.compilation;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.galasa.galasaecosystem.ILocalEcosystem;

public abstract class AbstractCompilationLocalZip extends AbstractCompilationLocal {
	
	/*
     * Within the specified file, this method replaces occurrences of mavenCentral() with the 
     * appropriate local maven repository closure.
     */
    protected void updateMavenRepo(Path fileToChange) throws IOException {
		String fileData = new String(Files.readAllBytes(fileToChange), Charset.defaultCharset());
    	
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

}
