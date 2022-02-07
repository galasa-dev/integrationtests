/*
 * Copyright contributors to the Galasa project
 */

package dev.galasa.inttests.ceda;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;

import dev.galasa.BeforeClass;
import dev.galasa.Test;
import dev.galasa.core.manager.IResourceString;
import dev.galasa.core.manager.ResourceString;
import dev.galasa.galasaecosystem.GalasaEcosystemManagerException;
import dev.galasa.galasaecosystem.IGenericEcosystem;

public abstract class AbstractCEDALocal {
	
	@ResourceString(tag = "PROG1", length = 8)
    public IResourceString resourceString1;
	
	@ResourceString(tag = "PROG2", length = 8)
    public IResourceString resourceString2;
	
	@ResourceString(tag = "TRX", length = 4)
    public IResourceString resourceString3;
	
	@ResourceString(tag = "LIB", length = 4)
    public IResourceString resourceString4;
	
	@ResourceString(tag = "GROUP1", length = 8)
    public IResourceString resourceString5;
	
	@ResourceString(tag = "GROUP2", length = 8)
    public IResourceString resourceString6;
	
	@BeforeClass
    public void setUp() throws GalasaEcosystemManagerException {
    	getEcosystem().setCpsProperty("cicsts.dse.tag.PRIMARY.applid", "IYK2ZNB5");
        getEcosystem().setCpsProperty("cicsts.provision.type", "DSE");
        getEcosystem().setCpsProperty("cicsts.default.logon.initial.text", "HIT ENTER FOR LATEST STATUS");
        getEcosystem().setCpsProperty("cicsts.default.logon.gm.text", "******\\(R)");
        
        getEcosystem().setCpsProperty("test.IVT.RESOURCE.STRING.PROG1", resourceString1.toString());
        getEcosystem().setCpsProperty("test.IVT.RESOURCE.STRING.PROG2", resourceString2.toString());
        getEcosystem().setCpsProperty("test.IVT.RESOURCE.STRING.TRX", resourceString3.toString());
        getEcosystem().setCpsProperty("test.IVT.RESOURCE.STRING.LIB", resourceString4.toString());
        getEcosystem().setCpsProperty("test.IVT.RESOURCE.STRING.GROUP1", resourceString5.toString());
        getEcosystem().setCpsProperty("test.IVT.RESOURCE.STRING.GROUP2", resourceString6.toString());
    }

    @Test
    public void testCEDAIvtTest() throws Exception {

        String runName = getEcosystem().submitRun(null, 
                null, 
                null, 
                "dev.galasa.cicsts.ceda.manager.ivt",
                "dev.galasa.cicsts.ceda.manager.ivt.CedaManagerIVT", 
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