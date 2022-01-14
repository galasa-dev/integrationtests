/*
 * Copyright contributors to the Galasa project
 */
package dev.galasa.inttests.docker;

import org.apache.commons.logging.Log;

import dev.galasa.core.manager.Logger;
import dev.galasa.ipnetwork.ICommandShell;

public abstract class AbstractDocker {
	
	@Logger
	private Log logger;
	private ICommandShell shell;
	
	protected void setShell(ICommandShell shell) throws Exception {
		this.shell = shell;
	}
	
	protected void updatePackageManager(String command) throws Exception {
		shell.issueCommand(command);
	}
	
	protected boolean isDockerInstalled() throws Exception {
		logger.info("Checking Docker is installed.");
		String res = shell.issueCommand("docker -v");	
		if(res.contains("no such file or directory")) {
			return false;
		} else {
			return true;
		}
	}
	
	protected void installDocker(String command) throws Exception {
		logger.info("Installing Docker");
		shell.issueCommand(command);
	}
	
	protected boolean isDockerRunning(String command) throws Exception {
		logger.info("Checking Docker is currently running");
		String res = shell.issueCommand(command);
		if(res.contains("active")){
			return true;
		} else {
			return false;
		}
	}
	
	protected void startDocker(String command) throws Exception {
		logger.info("Starting Docker");
		shell.issueCommand(command);
	}
	
	protected boolean isMavenInstalled() throws Exception {
		String res = shell.issueCommand("mvn -v");	
		if(res.contains("no such file or directory") || res.contains("not found")) {
			return false;
		} else {
			return true;
		}
	}
	
	protected void installMaven(String command) throws Exception {
		shell.issueCommand(command);
	}
}
