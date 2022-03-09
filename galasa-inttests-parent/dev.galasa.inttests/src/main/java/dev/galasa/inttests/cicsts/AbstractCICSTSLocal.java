/*
 * Copyright contributors to the Galasa project
 */

package dev.galasa.inttests.cicsts;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;

import dev.galasa.BeforeClass;
import dev.galasa.Test;
import dev.galasa.galasaecosystem.GalasaEcosystemManagerException;
import dev.galasa.galasaecosystem.IGenericEcosystem;
import dev.galasa.zos.IZosImage;

public abstract class AbstractCICSTSLocal {
	
	@BeforeClass
	public void setUp() throws GalasaEcosystemManagerException {
        getEcosystem().setCpsProperty("cicsts.provision.type", "SEM");
        getEcosystem().setCpsProperty("cicsts.default.logon.initial.text", "HIT ENTER FOR LATEST STATUS");
        getEcosystem().setCpsProperty("cicsts.default.logon.gm.text", "******\\(R)");	
        
        getEcosystem().setCpsProperty("zosprogram.cobol." + getZosImage().getImageID() + ".dataset.prefix", 
				  					  getEcosystem().getHostCpsProperty("zosprogram", "cobol", "dataset.prefix", getZosImage().getImageID()));
        getEcosystem().setCpsProperty("zosprogram.le." + getZosImage().getImageID() + ".dataset.prefix", 
				  					  getEcosystem().getHostCpsProperty("zosprogram", "le", "dataset.prefix", getZosImage().getImageID()));
        getEcosystem().setCpsProperty("zosprogram.cics." + getZosImage().getImageID() + ".dataset.prefix", 
				  					  getEcosystem().getHostCpsProperty("zosprogram", "cics", "dataset.prefix", getZosImage().getImageID()));
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
    
    abstract protected IZosImage getZosImage();
    
}