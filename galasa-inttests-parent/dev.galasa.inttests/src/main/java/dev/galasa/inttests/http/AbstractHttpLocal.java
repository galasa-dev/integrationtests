/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.inttests.http;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;

import dev.galasa.Test;
import dev.galasa.galasaecosystem.IGenericEcosystem;

public abstract class AbstractHttpLocal {
    
    @Test
    public void testHttpIvtTest() throws Exception {
        
        String runName = getEcosystem().submitRun(null, 
                null, 
                null, 
                "dev.galasa.http.manager.ivt", 
                "dev.galasa.http.manager.ivt.HttpManagerIVT", 
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
