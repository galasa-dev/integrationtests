/*
 * Copyright contributors to the Galasa project
 */

package dev.galasa.inttests.cicsts;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;

import dev.galasa.Test;
import dev.galasa.galasaecosystem.IGenericEcosystem;

public abstract class AbstractCICSTSLocal {

    @Test
    public void testCICSTSIvtTest() throws Exception {

        getEcosystem().setCpsProperty("cicsts.dse.tag.PRIMARY.applid", "IYK2ZNB5");
        getEcosystem().setCpsProperty("cicsts.provision.type", "DSE");
        getEcosystem().setCpsProperty("cicsts.default.logon.initial.text", "HIT ENTER FOR LATEST STATUS");
        getEcosystem().setCpsProperty("cicsts.default.logon.gm.text", "******\\(R)");

        String runName = getEcosystem().submitRun(null, 
                null, 
                null, 
                "dev.galasa.cicsts.manager.ivt",
                "dev.galasa.cicsts.manager.ivt.CICSTSManagerIVT", 
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