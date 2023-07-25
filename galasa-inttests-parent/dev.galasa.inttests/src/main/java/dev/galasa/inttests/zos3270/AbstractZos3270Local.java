/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.inttests.zos3270;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;

import dev.galasa.BeforeClass;
import dev.galasa.Test;
import dev.galasa.core.manager.CoreManager;
import dev.galasa.core.manager.ICoreManager;
import dev.galasa.galasaecosystem.GalasaEcosystemManagerException;
import dev.galasa.galasaecosystem.IGenericEcosystem;

public abstract class AbstractZos3270Local {
	
	@CoreManager
	public ICoreManager coreManager;
			
	
	@BeforeClass
	public void setupRunID() throws GalasaEcosystemManagerException {
		String runName = coreManager.getRunName();
		getEcosystem().setCpsProperty("test.IVT.RUN.NAME", runName);
	}
    
    @Test
    public void testZos3270IvtTest() throws Exception {
        
        String runName = getEcosystem().submitRun(null, 
                null, 
                null, 
                "dev.galasa.zos3270.manager.ivt", 
                "dev.galasa.zos3270.manager.ivt.Zos3270IVT", 
                null, 
                null, 
                null, 
                null);
        
        JsonObject run = getEcosystem().waitForRun(runName);
        
        String result = run.get("result").getAsString();
        
        assertThat(result).as("The test indicates the test passes").isEqualTo("Passed");
    }

    abstract protected IGenericEcosystem getEcosystem();

}
