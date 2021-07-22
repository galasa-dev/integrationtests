/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2021.
 */
package dev.galasa.inttests.compilation.local.mvp;

import java.io.IOException;
import java.nio.file.Path;

import dev.galasa.Test;
import dev.galasa.TestAreas;
import dev.galasa.galasaecosystem.IGenericEcosystem;
import dev.galasa.galasaecosystem.ILocalEcosystem;
import dev.galasa.galasaecosystem.IsolationInstallation;
import dev.galasa.galasaecosystem.LocalEcosystem;
import dev.galasa.inttests.compilation.AbstractCompilationLocalZip;
import dev.galasa.java.JavaVersion;
import dev.galasa.java.ubuntu.IJavaUbuntuInstallation;
import dev.galasa.java.ubuntu.JavaUbuntuInstallation;
import dev.galasa.linux.ILinuxImage;
import dev.galasa.linux.LinuxImage;
import dev.galasa.linux.OperatingSystem;

@Test
@TestAreas({"compilation","localecosystem","java11","ubuntu","mvp"})
public class CompilationLocalJava11UbuntuMvp extends AbstractCompilationLocalZip {

    @LocalEcosystem(linuxImageTag = "PRIMARY", isolationInstallation = IsolationInstallation.Mvp)
    public ILocalEcosystem ecosystem;
    
    @LinuxImage(operatingSystem = OperatingSystem.ubuntu, capabilities = "isolated")
    public ILinuxImage linuxImage;
    
    @JavaUbuntuInstallation(javaVersion = JavaVersion.v11)
    public IJavaUbuntuInstallation java;
    
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
		addManagerDependencies(testBuildGradle, allManagers);
		addDependencyConstraints(testBuildGradle);
		addImplementationConstraints(testBuildGradle);
		
    }

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

}
