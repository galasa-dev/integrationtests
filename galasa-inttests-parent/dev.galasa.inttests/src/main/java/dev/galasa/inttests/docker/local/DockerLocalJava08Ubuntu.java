package dev.galasa.inttests.docker.local;

import org.apache.commons.logging.Log;

import dev.galasa.BeforeClass;
import dev.galasa.Test;
import dev.galasa.TestAreas;
import dev.galasa.core.manager.Logger;
import dev.galasa.galasaecosystem.IGenericEcosystem;
import dev.galasa.galasaecosystem.ILocalEcosystem;
import dev.galasa.galasaecosystem.LocalEcosystem;
import dev.galasa.inttests.docker.AbstractDockerLocal;
import dev.galasa.ipnetwork.ICommandShell;
import dev.galasa.java.JavaVersion;
import dev.galasa.java.ubuntu.IJavaUbuntuInstallation;
import dev.galasa.java.ubuntu.JavaUbuntuInstallation;
import dev.galasa.linux.ILinuxImage;
import dev.galasa.linux.LinuxImage;
import dev.galasa.linux.OperatingSystem;

@Test
@TestAreas({"dockermanager", "localecosystem", "java08", "ubuntu"})
public class DockerLocalJava08Ubuntu extends AbstractDockerLocal {
	
	@LocalEcosystem(linuxImageTag = "PRIMARY")
	public ILocalEcosystem ecosystem;
	
	@LinuxImage(operatingSystem = OperatingSystem.ubuntu)
    public ILinuxImage linuxImage;
	
	@JavaUbuntuInstallation(javaTag = "PRIMARY", javaVersion = JavaVersion.v8)
	public IJavaUbuntuInstallation java;
	
	@Logger
	public Log testLogger;
	private ICommandShell shell;
	
	@BeforeClass
	public void setProps() throws Exception {
		ecosystem.setCpsProperty("docker.default.engines", "DKRENGINE01");
		ecosystem.setCpsProperty("docker.engine.DKRENGINE01.hostname", "192.168.1.200");
		ecosystem.setCpsProperty("docker.engine.DKRENGINE01.port", "3275");
		ecosystem.setCpsProperty("docker.engine.DKRENGINE01.max.slots", "1");
	}
	
	public void getShell() throws Exception {
		shell = linuxImage.getCommandShell();
		testLogger.info("Terminal access to test host obtained");
	}
	
	public void ensureRequrementsAreInstalled() throws Exception {
		
		String res = "";
		
		testLogger.info("Checking Docker is installed.");
		if(isDockerInstalled(shell)) {
			if(!isDockerRunning(shell)) {
				testLogger.info("Docker is not currently running. Starting Docker...");
				res = shell.issueCommand("sudo systemctl start docker");
				testLogger.info("Docker started.");
			}
		} else {
			res = this.updatePackageManager(shell);
			testLogger.info("Installing Docker...");
			res = shell.issueCommand("sudo apt -y install docker-ce docker-ce-cli containerd.io");
		}
		
		testLogger.info("Checking Maven is installed");
		if(!isMavenInstalled(shell)) {
			res = shell.issueCommand("sudo apt -y install maven");
		}
	}
	
	@Override
	protected String updatePackageManager(ICommandShell shell) throws Exception {
		testLogger.info("Updating package manager...");
		String res = shell.issueCommand("sudo apt -y update");
		return res;
	}
	
	@Override
	protected IGenericEcosystem getEcosystem() throws Exception{
		ecosystem.setCpsProperty(null, null);
		return ecosystem;
	}
}