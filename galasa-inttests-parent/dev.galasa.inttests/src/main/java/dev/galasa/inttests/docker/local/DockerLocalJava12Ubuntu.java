/*
 * Copyright contributors to the Galasa project
 */
package dev.galasa.inttests.docker.local;

import dev.galasa.BeforeClass;
import dev.galasa.Test;
import dev.galasa.TestAreas;
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
@TestAreas({"dockermanager", "localecosystem", "java12", "ubuntu"})
public class DockerLocalJava12Ubuntu extends AbstractDockerLocal {
	
	@LocalEcosystem(linuxImageTag = "PRIMARY")
	public ILocalEcosystem ecosystem;
	
	@LinuxImage(operatingSystem = OperatingSystem.ubuntu)
	public ILinuxImage linuxImage;
	
	@JavaUbuntuInstallation(javaVersion = JavaVersion.v12)
	public IJavaUbuntuInstallation java;
	
	@BeforeClass
	public void setProps() throws Exception {
		ecosystem.setCpsProperty("docker.default.engines", "DKRENGINE01");
		ecosystem.setCpsProperty("docker.engine.DKRENGINE01.hostname", "https://dkrengine01.cics-ts.hur.hdclab.intranet.ibm.com");
		ecosystem.setCpsProperty("docker.engine.DKRENGINE01.port", "2376");
		ecosystem.setCpsProperty("docker.engine.DKRENGINE01.max.slots", "3");
	}
	
	public void getShell() throws Exception {
		ICommandShell shell = linuxImage.getCommandShell();
		setShell(shell);
	}
	
	public void ensureRequrementsAreInstalled() throws Exception {
		if(isDockerInstalled()) {
			if(!isDockerRunning("sudo systemctl show --property ActiveState docker")) {
				startDocker("sudo systemctl start docker");
			}
		} else {
			updatePackageManager("sudo apt -y update");
			installDocker("sudo apt -y install docker-ce docker-ce-cli containerd.io");
		}
		
		if(!isMavenInstalled()) {
			installMaven("sudo apt -y install maven");
		}
	}
	
	@Override
	protected IGenericEcosystem getEcosystem() throws Exception{
		return ecosystem;
	}
}
