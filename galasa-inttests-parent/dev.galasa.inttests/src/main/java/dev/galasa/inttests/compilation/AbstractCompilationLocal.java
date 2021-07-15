/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2021.
 */
package dev.galasa.inttests.compilation;

import static org.assertj.core.api.Assertions.assertThat;

import dev.galasa.Test;
import dev.galasa.galasaecosystem.IGenericEcosystem;
import dev.galasa.java.ubuntu.IJavaUbuntuInstallation;
import dev.galasa.linux.ILinuxImage;

public abstract class AbstractCompilationLocal {

    abstract protected IGenericEcosystem getEcosystem();

    abstract protected ILinuxImage getLinuxImage();

    abstract protected IJavaUbuntuInstallation getJavaInstallation();

}
