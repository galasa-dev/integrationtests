package dev.galasa.inttests.docker;

import dev.galasa.ipnetwork.ICommandShell;

public abstract class AbstractDocker {
	
	abstract protected String updatePackageManager(ICommandShell shell) throws Exception;
	
	protected boolean isDockerInstalled(ICommandShell shell) throws Exception {
		String res = shell.issueCommand("docker -v");	
		if(res.contains("no such file or directory")) {
			return false;
		} else {
			return true;
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
		String res = shell.issueCommand("mvn -v");	
		if(res.contains("no such file or directory") || res.contains("not found")) {
			return false;
		} else {
			return true;
		}
	}
}
