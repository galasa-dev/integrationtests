/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2021.
 */
package dev.galasa.inttests.compilation;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import dev.galasa.BeforeClass;
import dev.galasa.Test;
import dev.galasa.galasaecosystem.IGenericEcosystem;
import dev.galasa.java.ubuntu.IJavaUbuntuInstallation;
import dev.galasa.linux.ILinuxImage;

public abstract class AbstractCompilationLocal {
	
	/*
	 * TODO
	 * [ ] Download Simplatform
	 * [ ] Unzip Simplatform
	 * [ ] Move Simplatform files & code to working directory
	 * [ ] Managers - Change Prefix
	 * [ ] Tests - Change Prefix
	 * Zip:
	 * 	   [ ] Managers - Add PluginManagement Closure
	 *     [ ] Managers - Change Repositories 
	 *     [ ] Tests - Add PluginManagement Closure
	 *     [ ] Tests - Change Repositories
	 *     Isolated: 
	 *         [ ] Add all managers to test dependencies
	 * Test:
	 * [ ] Build Manager
	 * [ ] Build Tests
	 */
	
	

    abstract protected IGenericEcosystem getEcosystem();

    abstract protected ILinuxImage getLinuxImage();

    abstract protected IJavaUbuntuInstallation getJavaInstallation();

}
