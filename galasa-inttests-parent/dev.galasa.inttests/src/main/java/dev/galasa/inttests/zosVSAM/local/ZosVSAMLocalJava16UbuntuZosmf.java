/*
 * Copyright contributors to the Galasa project 
 */
package dev.galasa.inttests.zosVSAM.local;

import dev.galasa.Test;
import dev.galasa.TestAreas;
import dev.galasa.galasaecosystem.IGenericEcosystem;
import dev.galasa.galasaecosystem.ILocalEcosystem;
import dev.galasa.galasaecosystem.LocalEcosystem;
import dev.galasa.inttests.zosVSAM.AbstractZosVSAMLocalZosmf;
import dev.galasa.java.JavaVersion;
import dev.galasa.java.ubuntu.IJavaUbuntuInstallation;
import dev.galasa.java.ubuntu.JavaUbuntuInstallation;
import dev.galasa.linux.ILinuxImage;
import dev.galasa.linux.LinuxImage;
import dev.galasa.linux.OperatingSystem;
import dev.galasa.zos.IZosImage;
import dev.galasa.zos.ZosImage;

@Test
@TestAreas({"zosManager","localecosystem","java16","ubuntu"})
public class ZosVSAMLocalJava16UbuntuZosmf extends AbstractZosVSAMLocalZosmf {

    @LocalEcosystem(linuxImageTag = "PRIMARY", addDefaultZosImage = "PRIMARY")
    public ILocalEcosystem ecosystem;
    
    @LinuxImage(operatingSystem = OperatingSystem.ubuntu)
    public ILinuxImage linuxImage;
    
    @JavaUbuntuInstallation(javaVersion = JavaVersion.v16)
    public IJavaUbuntuInstallation java;

    @ZosImage
    public IZosImage zosImage;

    @Override
    protected IGenericEcosystem getEcosystem() {
        return this.ecosystem;
    }

}
