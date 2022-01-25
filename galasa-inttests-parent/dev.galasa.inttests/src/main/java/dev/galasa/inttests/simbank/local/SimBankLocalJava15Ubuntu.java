/*
* Copyright contributors to the Galasa project 
*/
package dev.galasa.inttests.simbank.local;

import dev.galasa.Test;
import dev.galasa.TestAreas;
import dev.galasa.galasaecosystem.ILocalEcosystem;
import dev.galasa.galasaecosystem.LocalEcosystem;
import dev.galasa.inttests.simbank.AbstractSimBankLocal;
import dev.galasa.java.JavaVersion;
import dev.galasa.java.ubuntu.IJavaUbuntuInstallation;
import dev.galasa.java.ubuntu.JavaUbuntuInstallation;
import dev.galasa.linux.ILinuxImage;
import dev.galasa.linux.LinuxImage;
import dev.galasa.linux.OperatingSystem;

@Test
@TestAreas({"simplatform","localecosystem","java15","ubuntu"})
public class SimBankLocalJava15Ubuntu extends AbstractSimBankLocal {

    @LocalEcosystem(linuxImageTag = "PRIMARY", startSimPlatform = true)
    public ILocalEcosystem ecosystem;
    
    @LinuxImage(operatingSystem = OperatingSystem.ubuntu)
    public ILinuxImage linuxImage;
    
    @JavaUbuntuInstallation(javaVersion = JavaVersion.v15)
    public IJavaUbuntuInstallation java;

    @Override
    protected ILocalEcosystem getEcosystem() {
        return this.ecosystem;
    }

}
