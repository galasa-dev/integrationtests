/*
 * Copyright contributors to the Galasa project 
 */
package dev.galasa.inttests.zosBatch;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;

import dev.galasa.Test;
import dev.galasa.galasaecosystem.IGenericEcosystem;

public abstract class AbstractZosBatchLocalRSE {
    
    @Test
    public void testZosBatchIvtTestRSE() throws Exception {
    	
    	//Use RSE batch
    	getEcosystem().setCpsProperty("zos.bundle.extra.batch.manager", "dev.galasa.zosbatch.rseapi.manager");
        
        String runName = getEcosystem().submitRun(null, 
                null, 
                null, 
                "dev.galasa.zos.manager.ivt", 
                "dev.galasa.zos.manager.ivt.ZosManagerBatchIVT", 
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
