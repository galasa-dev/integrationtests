/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.inttests.sharedenvs;

import org.apache.commons.logging.Log;

import dev.galasa.AfterClass;
import dev.galasa.SharedEnvironment;
import dev.galasa.Test;
import dev.galasa.core.manager.Logger;
import dev.galasa.galasaecosystem.IKubernetesEcosystem;
import dev.galasa.galasaecosystem.KubernetesEcosystem;
import dev.galasa.kubernetes.IKubernetesNamespace;
import dev.galasa.kubernetes.KubernetesNamespace;

@SharedEnvironment
public class KubernetesEcosystemEnvironment {

    @Logger
    public Log logger;
    
    @KubernetesNamespace(kubernetesNamespaceTag = "SHARED")
    public IKubernetesNamespace namespace;
    
    @KubernetesEcosystem(ecosystemNamespaceTag = "SHARED", kubernetesNamespaceTag = "SHARED")
    public IKubernetesEcosystem ecosystem;
    
    
    @Test
    public void testAllSetupOk() {
        //*** All must be up if we have got this far
        this.logger.info("The Kubernetes Ecosystem has been setup correctly");
    }
    
    @AfterClass
    public void tearDownMessage() {
        this.logger.info("Tearing down the Kubernetes Ecosystem");
    }

}
