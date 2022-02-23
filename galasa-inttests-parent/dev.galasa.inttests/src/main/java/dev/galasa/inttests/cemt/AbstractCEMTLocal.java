/*
 * Copyright contributors to the Galasa project
 */

package dev.galasa.inttests.cemt;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;

import dev.galasa.BeforeClass;
import dev.galasa.Test;
import dev.galasa.core.manager.ResourceString;
import dev.galasa.core.manager.IResourceString;
import dev.galasa.galasaecosystem.GalasaEcosystemManagerException;
import dev.galasa.galasaecosystem.IGenericEcosystem;

public abstract class AbstractCEMTLocal {

    @ResourceString(tag = "PROG", length = 8)
    public IResourceString resourceString1;
	
	@ResourceString(tag = "TRX", length = 4)
    public IResourceString resourceString2;
	
	@ResourceString(tag = "GROUP", length = 8)
    public IResourceString resourceString3;
	
	@ResourceString(tag = "TDQ", length = 4)
    public IResourceString resourceString4;
    
    @BeforeClass
    public void setUp() throws GalasaEcosystemManagerException {
    	getEcosystem().setCpsProperty("cicsts.dse.tag.PRIMARY.applid", "IYK2ZNB5");
        getEcosystem().setCpsProperty("cicsts.provision.type", "DSE");
        getEcosystem().setCpsProperty("cicsts.default.logon.initial.text", "HIT ENTER FOR LATEST STATUS");
        getEcosystem().setCpsProperty("cicsts.default.logon.gm.text", "******\\(R)");
        
        getEcosystem().setCpsProperty("test.IVT.RESOURCE.STRING.PROG", resourceString1.toString());
        getEcosystem().setCpsProperty("test.IVT.RESOURCE.STRING.TRX", resourceString2.toString());
        getEcosystem().setCpsProperty("test.IVT.RESOURCE.STRING.GROUP", resourceString3.toString());
        getEcosystem().setCpsProperty("test.IVT.RESOURCE.STRING.TDQ", resourceString4.toString());
    }

    @Test
    public void testCEMTIvtTest() throws Exception {

        String runName = getEcosystem().submitRun(null, 
                null, 
                null, 
                "dev.galasa.cicsts.cemt.manager.ivt",
                "dev.galasa.cicsts.cemt.manager.ivt.CEMTManagerIVT", 
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