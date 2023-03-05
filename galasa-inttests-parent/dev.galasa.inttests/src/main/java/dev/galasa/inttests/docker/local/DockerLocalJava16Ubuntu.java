/*
 * Copyright contributors to the Galasa project
 */
package dev.galasa.inttests.docker.local;

import dev.galasa.Test;
import dev.galasa.TestAreas;
import dev.galasa.galasaecosystem.IGenericEcosystem;
import dev.galasa.galasaecosystem.ILocalEcosystem;
import dev.galasa.galasaecosystem.LocalEcosystem;
import dev.galasa.inttests.docker.AbstractDockerUbuntuLocal;
import dev.galasa.java.JavaVersion;
import dev.galasa.java.ubuntu.IJavaUbuntuInstallation;
import dev.galasa.java.ubuntu.JavaUbuntuInstallation;
import dev.galasa.linux.ILinuxImage;
import dev.galasa.linux.LinuxImage;
import dev.galasa.linux.OperatingSystem;

@Test
@TestAreas({"dockermanager", "localecosystem", "java16", "ubuntu"})
public class DockerLocalJava16Ubuntu extends AbstractDockerUbuntuLocal {
	
	@LocalEcosystem(linuxImageTag = "PRIMARY")
	public ILocalEcosystem ecosystem;
	
	@LinuxImage(imageTag = "PRIMARY")
	public ILinuxImage ecosystemLinuxImage;
	
	@JavaUbuntuInstallation(imageTag = "PRIMARY", javaVersion = JavaVersion.v16)
	public IJavaUbuntuInstallation java;
	
	@LinuxImage(imageTag = "DOCKER", operatingSystem = OperatingSystem.ubuntu, capabilities = {"nonshared"})
	public ILinuxImage dockerLinuxImage;
	
	@Override
	protected ILinuxImage getDockerLinuxImage() throws Exception{
		return dockerLinuxImage;
	}
	
	@Override
	protected IGenericEcosystem getEcosystem() throws Exception{
		return ecosystem;
	}
}
