/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.inttests.simbank;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.logging.Log;

import com.google.gson.JsonObject;

import dev.galasa.Test;
import dev.galasa.core.manager.Logger;
import dev.galasa.galasaecosystem.GalasaEcosystemManagerException;
import dev.galasa.galasaecosystem.ILocalEcosystem;
import dev.galasa.inttests.TestException;
import dev.galasa.ipnetwork.IpNetworkManagerException;

public abstract class AbstractSimBankLocal {
    
    @Logger
    public Log logger;

   @Test
    public void testSimbankIvt() throws GalasaEcosystemManagerException, InterruptedException, TestException, IpNetworkManagerException {

       String runName = getEcosystem().submitRun(null, 
               null, 
               null, 
               "dev.galasa.simbank.tests", 
               "dev.galasa.simbank.tests.SimBankIVT", 
               null, 
               null, 
               "simbank", 
               null);
       
       JsonObject run = getEcosystem().waitForRun(runName);
       
       String result = run.get("result").getAsString();
       
       assertThat(result).as("The test indicates the test passes").isEqualTo("Passed");
       
       logger.info("cache diags\n" + getEcosystem().getCommandShell().issueCommand("ls -l .galasa/felix-cache/"));
    }
    
    @Test
    public void testBasicAccountCreditTest() throws GalasaEcosystemManagerException, InterruptedException, TestException, IpNetworkManagerException {
        logger.info("cache diags\n" + getEcosystem().getCommandShell().issueCommand("ls -l .galasa/felix-cache/"));

        String runName = getEcosystem().submitRun(null, 
                null, 
                null, 
                "dev.galasa.simbank.tests", 
                "dev.galasa.simbank.tests.BasicAccountCreditTest", 
                null, 
                null, 
                "simbank", 
                null);
        
        JsonObject run = getEcosystem().waitForRun(runName);
        
        String result = run.get("result").getAsString();
        
        assertThat(result).as("The test indicates the test passes").isEqualTo("Passed");
        logger.info("cache diags\n" + getEcosystem().getCommandShell().issueCommand("ls -l .galasa/felix-cache/"));
    }
    
    @Test
    public void testProvisionedAccountCreditTests() throws GalasaEcosystemManagerException, InterruptedException, TestException, IpNetworkManagerException {
        logger.info("cache diags\n" + getEcosystem().getCommandShell().issueCommand("ls -l .galasa/felix-cache/"));

        String runName = getEcosystem().submitRun(null, 
                null, 
                null, 
                "dev.galasa.simbank.tests", 
                "dev.galasa.simbank.tests.ProvisionedAccountCreditTests", 
                null, 
                null, 
                "simbank", 
                null);
        
        JsonObject run = getEcosystem().waitForRun(runName);
        
        String result = run.get("result").getAsString();
        
        assertThat(result).as("The test indicates the test passes").isEqualTo("Passed");
        logger.info("cache diags\n" + getEcosystem().getCommandShell().issueCommand("ls -l .galasa/felix-cache/"));
    }
    
    abstract protected ILocalEcosystem getEcosystem();

}
