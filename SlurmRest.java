package org.texttechnologylab.DockerUnifiedUIMAInterface.driver.slurmInDocker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.DockerClientBuilder;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SlurmRest {
    private DockerClient dockerClient = DockerClientBuilder.getInstance().build();
    private final String RESTVERSION = "v0.0.42";
    private final String URL = "http://localhost:6820/slurm/" + RESTVERSION+"/";
    OkHttpClient httpClient = new OkHttpClient();


    public Map<String, String> containerNameID() {
        Map<String, String> nameID = new HashMap<>();
        List<Container> exec = dockerClient.listContainersCmd().exec();
        exec.stream().forEach((c) -> {
            String[] names = c.getNames();// [/xxx]
            System.out.println(names[0]);
            Pattern p = Pattern.compile("^/([a-zA-Z0-9]*)$");
            Matcher m = p.matcher(names[0]);
            String result = m.find() ? m.group(1) : "";
            String id = c.getId();
            nameID.put(result, id);
        });
        return nameID;
    }

    public List<String> listContainerNames() {
        List<Container> exec = dockerClient.listContainersCmd().exec();
        List<String> containers = new ArrayList<>();
        exec.stream().forEach(container -> {
            containers.add(Arrays.toString(container.getNames()));
        });
        return containers;
    }

    public boolean checkRESTD() {
        List<String> containers = listContainerNames();
        return containers.stream().anyMatch(containerName -> containerName.contains("rest"));
    }

    public String showHostName() throws IOException {
        ProcessBuilder pb = new ProcessBuilder();
        pb.command("whoami");
        Process start = pb.redirectErrorStream(true).start();
        BufferedReader br = new BufferedReader(new InputStreamReader(start.getInputStream()));
        return br.readLine();
    }

    public String generateTokenByHost(String containerName) throws IOException, InterruptedException {
        String hostName = showHostName();
        String arg = "username=".concat(hostName);
        String[] comms = new String[]{"scontrol", "token", arg};
        // default life-time 24h
        String token = executeInContainer(containerName, comms);
        String[] split = token.split("=", 2);
        return split[1].trim();
    }

    public String executeInContainer(String containerName, String[] commands) throws IOException, InterruptedException {
        Map<String, String> nameIDMap = containerNameID();
        String containerId = nameIDMap.get(containerName);
        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withCmd(commands)
                .exec();

        StringBuilder result = new StringBuilder();
        dockerClient.execStartCmd(execCreateCmdResponse.getId())
                .exec(new ResultCallback.Adapter<Frame>() {
                    @Override
                    public void onNext(Frame frame) {
                        result.append(new String(frame.getPayload()));
                    }
                }).awaitCompletion();
        return result.toString();
    }


    public String query(String token, String where) throws IOException, InterruptedException {
        Request req = new Request.Builder().url(URL.concat(where)).header("X-SLURM-USER-TOKEN", token).get().build();
        try (Response response = httpClient.newCall(req).execute()) {
            return response.body().string();
        }

    }

}


