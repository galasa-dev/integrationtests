/*
* Copyright contributors to the Galasa project 
*/
package dev.galasa.inttests.artifact.kubernetes;

import java.util.UUID;

import org.apache.commons.logging.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.galasa.Summary;
import dev.galasa.Tags;
import dev.galasa.Test;
import dev.galasa.TestAreas;
import dev.galasa.core.manager.Logger;
import dev.galasa.core.manager.RunName;
import dev.galasa.galasaecosystem.GalasaEcosystemManagerException;
import dev.galasa.galasaecosystem.IKubernetesEcosystem;
import dev.galasa.galasaecosystem.KubernetesEcosystem;
import dev.galasa.inttests.TestException;
import dev.galasa.kubernetes.IKubernetesNamespace;
import dev.galasa.kubernetes.KubernetesNamespace;

@Test
@Summary("Run Artifact IVT tests in a provisioned Galasa Ecosystem")
@TestAreas({"artifactManager","kubernetes","java08"})
@Tags({"pipelinetest"})
public class ArtifactKubernetesJava08 {

    @Logger
    public Log logger;
    
    @RunName
    public String runName;
    
    @KubernetesNamespace(kubernetesNamespaceTag = "SHARED")
    public IKubernetesNamespace namespace;
    
    @KubernetesEcosystem(ecosystemNamespaceTag = "SHARED", kubernetesNamespaceTag = "SHARED")
    public IKubernetesEcosystem ecosystem;
    
    
    @Test
    public void testArtifactManagerIVT() throws GalasaEcosystemManagerException, InterruptedException, TestException {

        submitTest("dev.galasa.artifact.manager.ivt.ArtifactManagerIVT");
        
        logger.info("ArtifactManagerIVT test passed");
    }
    
    
    private void submitTest(String testName) throws GalasaEcosystemManagerException, TestException {
        String groupName = UUID.randomUUID().toString();
        
        logger.info("Submitting test " + testName);
        
        ecosystem.submitRun(null, 
                runName,
                groupName, 
                "dev.galasa.artifact.manager.ivt", 
                testName, 
                null, 
                null, 
                null, 
                null);
        
        JsonObject finalResponse = ecosystem.waitForGroupNames(groupName, 180);
        JsonArray runs = finalResponse.getAsJsonArray("runs");
        if (runs == null || runs.size() == 0) {
            throw new TestException("Lost the " + testName + " test, last response was:-\n" + finalResponse);
        }
        
        if (runs.size() != 1) {
            throw new TestException("Too many runs returned, last response was:-\n" + finalResponse);
        }
                
        JsonObject run = runs.get(0).getAsJsonObject();
        JsonElement result = run.get("result");
        if (result == null) {
            throw new TestException("Run did return a result, last response was:-\n" + finalResponse);
        }
        
        if (!"Passed".equals(result.getAsString())) {
            throw new TestException("Run did not pass, last response was:-\n" + finalResponse);
        }   
    }
    

    
}
