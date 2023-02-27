/*
 * Copyright contributors to the Galasa project
 */
package dev.galasa.inttests.docker;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.apache.commons.io.IOUtils;

import dev.galasa.BeforeClass;
import dev.galasa.ResultArchiveStoreContentType;
import dev.galasa.SetContentType;
import dev.galasa.core.manager.StoredArtifactRoot;
import dev.galasa.githubissue.GitHubIssue;

@GitHubIssue( issue = "1366" )
public abstract class AbstractDockerUbuntuLocal extends AbstractDocker {
	
	@StoredArtifactRoot
	public Path storedArtifactRoot;
	
	@BeforeClass
	public void setupEnvironment() throws Exception {
		shell = getDockerLinuxImage().getCommandShell();
		
		logger.info("Checking if docker already exists");
		String res = shell.issueCommand("sudo docker ps");
		if (res.contains("CONTAINER ID")) {
			logger.info("Docker is already installed. Skipping install step");
		} else {
			installDocker();
		}
			
		Path servicePath = getDockerLinuxImage().getRoot().resolve("lib/systemd/system/docker.service");
		Path homePath = getDockerLinuxImage().getHome().resolve("docker.service");
		Path etcServicePath = getDockerLinuxImage().getRoot().resolve("/etc/systemd/system/docker.service");
		
		if (Files.exists(etcServicePath)) {
			String dockerEtcServiceContents = IOUtils.toString(Files.newInputStream(etcServicePath, StandardOpenOption.READ), StandardCharsets.UTF_8);
			if (dockerEtcServiceContents.contains("tcp://0.0.0.0:" + DOCKER_PORT)) {
				logger.info("Docker service already exposed. Skipping Expose step");
				return;
			}
		} else {
			logger.info("Exposing Docker service");
			if (Files.exists(servicePath)) {
				String dockerServiceContents = IOUtils.toString(Files.newInputStream(servicePath, StandardOpenOption.READ), StandardCharsets.UTF_8);		
				if (!dockerServiceContents.contains("tcp://0.0.0.0:")) {
					dockerServiceContents = dockerServiceContents.replace("-H fd://", "-H fd:// -H tcp://0.0.0.0:" + DOCKER_PORT);
				}
				Files.write(homePath, dockerServiceContents.getBytes(), StandardOpenOption.WRITE);
			} else {
				throw new Exception("No 'docker.service' file to modify");
			}

			res = shell.issueCommand("sudo mv " + homePath.toString() + " /etc/systemd/system/docker.service && echo \"Exit code:\" $?");
			storeOutput("ecosystem/dockerSetup", "serviceFileMove.txt", "Command: sudo mv \" + homePath.toString() + \" /etc/systemd/system/docker.service \nOutput:\n" + res);
			if (!res.contains("Exit code: 0")) {
				logger.error("Moving updated service file failed");
				throw new Exception("Moving updated service file failed: " + res);
			}
			logger.info("Restarting daemon and docker services...");
			res = shell.issueCommand("sudo systemctl daemon-reload && echo \"Exit code:\" $?");
			storeOutput("ecosystem/dockerSetup", "daemonReload.txt", "Command: sudo systemctl daemon-reload \nOutput:\n" + res);
			if (!res.contains("Exit code: 0")) {
				logger.error("Restarting daemon failed");
				throw new Exception("Restarting daemon failed with return code: " + res);
			}
			res = shell.issueCommand("sudo systemctl restart docker && echo \"Exit code:\" $?");
			storeOutput("ecosystem/dockerSetup", "dockerSetup.txt", "Command: sudo systemctl restart docker \nOutput:\n" + res);
			if (!res.contains("Exit code: 0")) {
				logger.error("Restarting docker service failed");
				throw new Exception("Restarting docker service failed with return code: " + res);
			}
		}
	}
	
	private void installDocker() throws Exception {
		logger.info("Updating package manager docker installation");
		String res = shell.issueCommand("sudo apt-get update && echo \"Exit code:\" $?");
		storeOutput("ecosystem/dockerSetup", "aptUpdate.txt", "Command: sudo apt update \nOutput:\n" + res);
		if (!res.contains("Exit code: 0")) {
			logger.error("Updating package manager failed");
			throw new Exception("Package manager could not be updated: " + res);
		}
		logger.info("Installing Docker...");
		res = shell.issueCommand("sudo apt-get -y install docker.io");
		storeOutput("ecosystem/dockerSetup", "installDocker.txt", "Command: sudo apt-get -y install docker.io \nOutput:\n" + res);
		if (!res.contains("Created symlink /etc/systemd/system/sockets.target.wants/docker.socket")) {
			logger.error("Installing docker.io failed");
			throw new Exception("Could not install docker.io: " + res); 
		}
		logger.info("Starting Docker service...");
		res = shell.issueCommand("sudo systemctl start docker");
		storeOutput("ecosystem/dockerSetup", "startDocker.txt", "Command: sudo systemctl start docker \nOutput:\n" + res);
		res = shell.issueCommand("sudo systemctl show --property ActiveState docker");	
		Instant expire = Instant.now().plus(3, ChronoUnit.MINUTES);
		Instant statusReport = Instant.now().plus(1, ChronoUnit.MINUTES);
		boolean started = false;
		while(Instant.now().isBefore(expire)) {
			if (Instant.now().isAfter(statusReport)) {
				logger.trace("Still waiting for Docker service to start...");
				statusReport = Instant.now().plus(1, ChronoUnit.MINUTES);
			}
			if (!res.contains("ActiveState=active") && !res.contains("ActiveState=activating")) {
				logger.error("Unknown active state");
				throw new Exception("Docker could not be started: " + res);
			} else if (res.contains("ActiveState=active")) {
				started = true;
				break;
			}
		}
		if (!started) {
			logger.error("Docker service did not start before the timeoout");
			throw new Exception("Docker service did not start before the timeout");
		}
	}
	
	private void storeOutput(String folder, String file, String content) throws IOException {	
		Path requestPath = storedArtifactRoot.resolve(folder).resolve(file);
        Files.write(requestPath, content.getBytes(), new SetContentType(ResultArchiveStoreContentType.TEXT),
                StandardOpenOption.CREATE);
    }
}
