/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.inttests.cicsts;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;

import dev.galasa.BeforeClass;
import dev.galasa.Test;
import dev.galasa.cicsts.CicsRegion;
import dev.galasa.cicsts.CicstsManagerException;
import dev.galasa.cicsts.ICicsRegion;
import dev.galasa.galasaecosystem.GalasaEcosystemManagerException;
import dev.galasa.galasaecosystem.IGenericEcosystem;

public abstract class AbstractCICSTSLocal {
	
	@CicsRegion(cicsTag = "A")
	public ICicsRegion cics;
	
	@BeforeClass
    public void setProperties() throws GalasaEcosystemManagerException, CicstsManagerException {
		// Setting these properties in the shadow ecosystem
		getEcosystem().setCpsProperty("cicsts.provision.type", "DSE");
		getEcosystem().setCpsProperty("cicsts.dse.tag.A.applid", cics.getApplid());
		getEcosystem().setCpsProperty("cicsts.dse.tag.A.version", cics.getVersion().toString());	
//		getEcosystem().setCpsProperty("cicsts.dse.tag.A.jvmprofiledir", cics.getJvmProfileDir());
//		getEcosystem().setCpsProperty("cicsts.dse.tag.A.usshome", cics.getUssHome());
//		getEcosystem().setCpsProperty("cicsts.dse.tag.A.javahome", cics.getJavaHome());	
		getEcosystem().setCpsProperty("cicsts.default.logon.initial.text", "HIT ENTER FOR LATEST STATUS");
		getEcosystem().setCpsProperty("cicsts.default.logon.gm.text", "******\\(R)");
	}
        
    @Test
    public void testCICSTSIvtTest() throws Exception {

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