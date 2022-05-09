/*
 * Copyright contributors to the Galasa project
 */
package dev.galasa.inttests.zos3270;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;

import dev.galasa.BeforeClass;
import dev.galasa.Test;
import dev.galasa.cicsts.CicsRegion;
import dev.galasa.cicsts.CicsTerminal;
import dev.galasa.cicsts.CicstsManagerException;
import dev.galasa.cicsts.ICicsRegion;
import dev.galasa.cicsts.ICicsTerminal;
import dev.galasa.core.manager.CoreManager;
import dev.galasa.core.manager.ICoreManager;
import dev.galasa.galasaecosystem.GalasaEcosystemManagerException;
import dev.galasa.galasaecosystem.IGenericEcosystem;

public abstract class AbstractZos3270Local {
	
	@CoreManager
	public ICoreManager coreManager;
	
	@CicsRegion(cicsTag = "A")
	public ICicsRegion cics;
	
	@CicsTerminal(cicsTag = "A")
	public ICicsTerminal terminal;
	
	@BeforeClass
    public void setProperties() throws GalasaEcosystemManagerException, CicstsManagerException {
		// Setting these properties in the shadow ecosystem
		getEcosystem().setCpsProperty("cicsts.provision.type", "DSE");
		getEcosystem().setCpsProperty("cicsts.dse.tag.A.applid", cics.getApplid());
		getEcosystem().setCpsProperty("cicsts.dse.tag.A.version", cics.getVersion().toString());	
		getEcosystem().setCpsProperty("cicsts.default.logon.initial.text", "HIT ENTER FOR LATEST STATUS");
		getEcosystem().setCpsProperty("cicsts.default.logon.gm.text", "******\\(R)");
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
