package utils;
/**
 * This the how the client will manage the docker instance
 *
 * @author Devam Sisodraker (devam@alumni.ubc.ca)
 */

import models.Project;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class DockerManager {

    private static Runtime runtime = null;
    private static final DockerManager INSTANCE = new DockerManager();
    private static boolean DOCKER_AVAILABLE = false;
    private static Project currentProject;

    private DockerManager() {
        this.runtime = Runtime.getRuntime();
        this.currentProject = null;
        DOCKER_AVAILABLE = this.checkDocker();
    }

    /**
     * Retrieves the singleton instance of the DockerManager
     *
     * @return Singleton instance of the DockerManager
     */
    public static DockerManager getInstance() {
        return INSTANCE;
    }

    /**
     * Retrieves the currently running project
     *
     * @return the currently running project
     */
    public Project getCurrentProject() {
        return currentProject;
    }


    /**
     * Runs a project and requests the docker instance to join a swarm
     *
     * @param project the project to run
     * @return Whether docker has successfully joined the swarm
     * @throws IOException
     */
    public boolean runProject(Project project) throws IOException {
        if (joinSwarm(project.getIp(), project.getPort(), project.getToken())) {
            this.currentProject = project;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check to see if docker is installed
     *
     * @return Whether the docker CLI has been installed on the machine
     */
    public boolean checkDocker() {
        try {
            Process process = this.runtime.exec("docker -v");
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.US_ASCII);

            if (output.startsWith("Docker version")) {
                DOCKER_AVAILABLE = true;
                return true;
            }
            DOCKER_AVAILABLE = false;
            return false;
        } catch (IOException exception) {
            DOCKER_AVAILABLE = false;
            return false;
        }
    }

    /**
     * Request the docker instance to join a swarm
     *
     * @param ip    Ip address of the leader node
     * @param port  port of the listening leader node
     * @param token Entry token for the worker to join
     * @return Whether docker has successfully joined the swarm
     * @throws IOException
     */
    public boolean joinSwarm(String ip, Integer port, String token) throws IOException {
        this.preCheckDocker();

        String command = "docker swarm join --token " + token + " " + ip + ":" + port.toString();
        Process process = this.runtime.exec(command);
        String out = new String(process.getInputStream().readAllBytes(), StandardCharsets.US_ASCII);

        if (out.startsWith("This node joined a swarm as a worker."))  {
            return true;
        }
        return false;
    }

    /**
     * Request the docker instance to leave the swarm
     *
     * @param force Force the docker instance to leave the swarm
     * @return Whether docker successfully left the swarm
     * @throws IOException
     */
    public boolean leaveSwarm(Boolean force) throws IOException {
        this.preCheckDocker();

        String command = force ? "docker swarm leave -f" : "docker swarm leave";
        Process process = this.runtime.exec(command);
        String out = new String(process.getInputStream().readAllBytes(), StandardCharsets.US_ASCII);

        if (out.startsWith("Node left the default swarm.")) {
            return true;
        }
        return false;
    }

    /**
     * Restart the docker Daemon
     * RESTRICTION: Only works if daemon is running in the same environment as this java VM
     *
     * @return Whether the restart was successful, if it returns False, get admin privileges
     * @throws IOException
     */
    public boolean restartDaemon() throws IOException {
        this.preCheckDocker();

        if (System.getProperty("os").startsWith("Windows")) {
            // Windows
            this.restartWindowsDaemon();
        } else {
            // POSIX
            this.restartPosixDaemon();
        }

        String command = "docker ps";
        Process process = this.runtime.exec(command);
        String out = new String(process.getInputStream().readAllBytes(), StandardCharsets.US_ASCII);

        if (out.toLowerCase().startsWith("error")) {
            return false;
        }
        return true;
    }

    private void restartPosixDaemon() throws IOException {
        String commandStop = "systemctl stop docker";
        Process processStop = this.runtime.exec(commandStop);
        String outStop = new String(processStop.getInputStream().readAllBytes(), StandardCharsets.US_ASCII);

        String commandStart = "systemctl start docker";
        Process processStart = this.runtime.exec(commandStart);
        String outStart = new String(processStart.getInputStream().readAllBytes(), StandardCharsets.US_ASCII);
    }

    private void restartWindowsDaemon() throws IOException {
        String commandStop = "net stop com.docker.service";
        Process processStop = this.runtime.exec(commandStop);
        String outStop = new String(processStop.getInputStream().readAllBytes(), StandardCharsets.US_ASCII);

        String commandStart = "net start com.docker.service";
        Process processStart = this.runtime.exec(commandStart);
        String outStart = new String(processStart.getInputStream().readAllBytes(), StandardCharsets.US_ASCII);
    }

    private void preCheckDocker() {
        if (!this.checkDocker()) {
            throw new Error("Docker is not accessible, make sure it is installed and added to the path.");
        }
    }


}
