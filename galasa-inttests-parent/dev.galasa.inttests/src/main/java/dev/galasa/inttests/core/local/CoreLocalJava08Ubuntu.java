/*
* Copyright contributors to the Galasa project 
*/
package dev.galasa.inttests.core.local;

import dev.galasa.Tags;
import dev.galasa.Test;
import dev.galasa.TestAreas;
import dev.galasa.galasaecosystem.IGenericEcosystem;
import dev.galasa.galasaecosystem.ILocalEcosystem;
import dev.galasa.galasaecosystem.LocalEcosystem;
import dev.galasa.inttests.core.AbstractCoreLocal;
import dev.galasa.java.JavaVersion;
import dev.galasa.java.ubuntu.IJavaUbuntuInstallation;
import dev.galasa.java.ubuntu.JavaUbuntuInstallation;
import dev.galasa.linux.ILinuxImage;
import dev.galasa.linux.LinuxImage;
import dev.galasa.linux.OperatingSystem;

@Test
@TestAreas({"coreManager","localecosystem","java08","ubuntu"})
@Tags({"pipelinetest"})
public class CoreLocalJava08Ubuntu extends AbstractCoreLocal {

    @LocalEcosystem(linuxImageTag = "PRIMARY")
    public ILocalEcosystem ecosystem;
    
    @LinuxImage(operatingSystem = OperatingSystem.ubuntu)
    public ILinuxImage linuxImage;
    
    @JavaUbuntuInstallation(javaVersion = JavaVersion.v8)
    public IJavaUbuntuInstallation java;

    @Override
    protected IGenericEcosystem getEcosystem() {
        return this.ecosystem;
    }

}
