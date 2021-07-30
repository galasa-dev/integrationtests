/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2021.
 */
package dev.galasa.inttests.compilation.offline.mvp;

import java.io.IOException;

import dev.galasa.Test;
import dev.galasa.TestAreas;
import dev.galasa.artifact.TestBundleResourceException;
import dev.galasa.galasaecosystem.IGenericEcosystem;
import dev.galasa.galasaecosystem.ILocalEcosystem;
import dev.galasa.galasaecosystem.IsolationInstallation;
import dev.galasa.galasaecosystem.LocalEcosystem;
import dev.galasa.inttests.compilation.offline.AbstractCompilationLocalOffline;
import dev.galasa.java.JavaVersion;
import dev.galasa.java.ubuntu.IJavaUbuntuInstallation;
import dev.galasa.java.ubuntu.JavaUbuntuInstallation;
import dev.galasa.linux.ILinuxImage;
import dev.galasa.linux.LinuxImage;
import dev.galasa.linux.OperatingSystem;

@Test
@TestAreas({"compilation","localecosystem","java08","ubuntu","mvp"})
public class CompilationLocalOfflineJava08UbuntuMVP extends AbstractCompilationLocalOffline {

    @LocalEcosystem(linuxImageTag = "PRIMARY", isolationInstallation = IsolationInstallation.Mvp)
    public ILocalEcosystem ecosystem;
    
    @LinuxImage(operatingSystem = OperatingSystem.ubuntu, capabilities = "mvp")
    public ILinuxImage linuxImage;
    
    @JavaUbuntuInstallation(javaVersion = JavaVersion.v8)
    public IJavaUbuntuInstallation java;
    
    @Override
    protected IGenericEcosystem getEcosystem() {
        return this.ecosystem;
    }

    @Override
    protected ILinuxImage getLinuxImage() {
        return this.linuxImage;
    }
    
    @Override
    protected IJavaUbuntuInstallation getJavaInstallation() {
        return this.java;
    }

    @Override
	protected String[] getManagers() throws TestBundleResourceException, IOException {
		
		String managerString = resources.retrieveFileAsString("offlinebuild/MVPManagers.txt"); 
		
		String[] managers = managerString.split("\\r?\\n");
		return managers;
	}

}
