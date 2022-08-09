/*
 * Copyright contributors to the Galasa project
 */
package dev.galasa.inttests.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;

import dev.galasa.Test;
import dev.galasa.galasaecosystem.IGenericEcosystem;

public abstract class AbstractCoreLocal {
    
    @Test
    public void testCoreIvtTest() throws Exception {
        
        String runName = getEcosystem().submitRun(null, 
                null, 
                null, 
                "dev.galasa.core.manager.ivt", 
                "dev.galasa.core.manager.ivt.CoreManagerIVT", 
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
