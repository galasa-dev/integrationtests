/*
 * Copyright contributors to the Galasa project
 */

package dev.galasa.inttests.ceci;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;

import dev.galasa.BeforeClass;
import dev.galasa.Test;
import dev.galasa.cicsts.CeciException;
import dev.galasa.cicsts.CemtException;
import dev.galasa.cicsts.CicsRegion;
import dev.galasa.cicsts.CicsTerminal;
import dev.galasa.cicsts.CicstsManagerException;
import dev.galasa.cicsts.ICicsRegion;
import dev.galasa.cicsts.ICicsTerminal;
import dev.galasa.galasaecosystem.GalasaEcosystemManagerException;
import dev.galasa.galasaecosystem.IGenericEcosystem;
import dev.galasa.zosprogram.IZosProgram;
import dev.galasa.zosprogram.ZosProgram;
import dev.galasa.zosprogram.ZosProgram.Language;

public abstract class AbstractCECILocal {
	
	@CicsRegion(cicsTag = "A")
	public ICicsRegion cics;
	
	@CicsTerminal(cicsTag = "A")
	public ICicsTerminal terminal;
	
	@ZosProgram(name = "APITEST", location = "/zosPrograms", language = Language.COBOL, imageTag = "PRIMARY", cics = true)
	public IZosProgram APITEST;
	   
	@ZosProgram(name = "CONTTEST", location = "/zosPrograms", language = Language.COBOL, imageTag = "PRIMARY", cics = true)
	public IZosProgram CONTTEST;
	   
	@ZosProgram(name = "PRGABEND", location = "/zosPrograms", language = Language.COBOL, imageTag = "PRIMARY", cics = true)
	public IZosProgram PRGABEND;
	
	public String libName = "LIB1";
	
	public String groupName = "PROGGRP";
	
	@BeforeClass
	public void loadResources() throws CeciException, CicstsManagerException {
        // Define compiled COBOL programs to CICS Region
		cics.ceda().createResource(terminal, "PROGRAM", APITEST.getName(), groupName, null);
		cics.ceda().createResource(terminal, "PROGRAM", CONTTEST.getName(), groupName, null);
        cics.ceda().createResource(terminal, "PROGRAM", PRGABEND.getName(), groupName, null);
	    // Define Library where all Programs are compiled
        cics.ceda().createResource(terminal, "LIBRARY", libName, groupName, "DSNAME01(" + APITEST.getLoadlib().getName() + ")");
        // Install everything    
        cics.ceda().installGroup(terminal, groupName);
	}
	
	@BeforeClass
	public void checkResourcesLoaded() throws CemtException, CicstsManagerException {
	    assertThat(cics.cemt().inquireResource(terminal, "PROGRAM", APITEST.getName()).get("program")).isEqualTo(APITEST.getName());
        assertThat(cics.cemt().inquireResource(terminal, "PROGRAM", CONTTEST.getName()).get("program")).isEqualTo(CONTTEST.getName());
        assertThat(cics.cemt().inquireResource(terminal, "PROGRAM", PRGABEND.getName()).get("program")).isEqualTo(PRGABEND.getName());
        assertThat(cics.cemt().inquireResource(terminal, "LIBRARY", libName).get("library")).isEqualTo(libName);  
    }
	
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
    public void testCECIIvtTest() throws Exception {

        String runName = getEcosystem().submitRun(null, 
                null, 
                null, 
                "dev.galasa.cicsts.ceci.manager.ivt",
                "dev.galasa.cicsts.ceci.manager.ivt.CECIManagerIVT", 
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