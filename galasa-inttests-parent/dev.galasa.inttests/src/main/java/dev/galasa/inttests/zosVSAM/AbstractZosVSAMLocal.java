/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2021.
 */
package dev.galasa.inttests.zosVSAM;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;

import dev.galasa.BeforeClass;
import dev.galasa.Test;
import dev.galasa.core.manager.CoreManager;
import dev.galasa.core.manager.ICoreManager;
import dev.galasa.galasaecosystem.GalasaEcosystemManagerException;
import dev.galasa.galasaecosystem.IGenericEcosystem;

public abstract class AbstractZosVSAMLocal {
	@CoreManager
	public ICoreManager coreManager;
			
	
	@BeforeClass
	public void setupRunID() throws GalasaEcosystemManagerException {
		String runName = coreManager.getRunName();
		getEcosystem().setCpsProperty("test.IVT.RUN.NAME", runName);
	}
	
    @Test
    public void testZosFileIvtTestZOSMF() throws Exception {
    	getEcosystem().setCpsProperty("zos.bundle.extra.file.manager", "dev.galasa.zosfile.zosmf.manager");
        
        String runName = getEcosystem().submitRun(null, 
                null, 
                null, 
                "dev.galasa.zos.manager.ivt", 
                "dev.galasa.zos.manager.ivt.ZosManagerFileVSAMIVT", 
                null, 
                null, 
                null, 
                null);
        
        JsonObject run = getEcosystem().waitForRun(runName);
        
        String result = run.get("result").getAsString();
        
        assertThat(result).as("The test indicates the test passes").isEqualTo("Passed");
    }
    
    @Test
    public void testZosFileIvtTestRSE() throws Exception {
    	getEcosystem().setCpsProperty("zos.bundle.extra.file.manager", "dev.galasa.zosfile.rseapi.manager");
        
        String runName = getEcosystem().submitRun(null, 
                null, 
                null, 
                "dev.galasa.zos.manager.ivt", 
                "dev.galasa.zos.manager.ivt.ZosManagerFileVSAMIVT", 
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
