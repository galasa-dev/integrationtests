package dev.galasa.inttests.docker;

import org.apache.commons.logging.Log;

import dev.galasa.core.manager.Logger;
import dev.galasa.ipnetwork.ICommandShell;

public abstract class AbstractDocker {
	
	@Logger
	public Log logger;
	
	abstract protected String updatePackageManager(ICommandShell shell) throws Exception;
	
	protected boolean isDockerInstalled(ICommandShell shell) throws Exception {
		String res = shell.issueCommand("docker -v; echo $?");	
		// Regex is splitting the string on new line characters. 
		// As of Java 8 \R pattern will match any Unicode line breaker.
		// Explained here: https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html
		String returnCode = res.split("\\R")[1];
		if(returnCode.equals("0")) {
			return true;
		} else {
			return false;
		}
	}
	
	protected boolean isDockerRunning(ICommandShell shell) throws Exception {
		String res = shell.issueCommand("systemctl show --property ActiveState docker");
		if(res.contains("active")){
			return true;
		} else {
			return false;
		}
	}
	
	protected boolean isMavenInstalled(ICommandShell shell) throws Exception {
		String res = shell.issueCommand("mvn -v; echo $?");
		// Regex is splitting the string on new line characters. 
		// As of Java 8 \R pattern will match any Unicode line breaker.
		// Explained here: https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html
		String returnCode = res.split("\\R")[1];
		if(returnCode.equals("0")) {
			return true;
		} else {
			return false;
		}
	}
}
