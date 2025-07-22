# DUUI with Slurm 

The DUUI-UIMA application runs continuously inside slurm and exposes a port for DUUI to call in java.

对于个人使用，可以不通过login节点，直接提交任务。

1 For personal use, tasks can be submitted directly without going through the login node.

对于第三方使用，管理员应该预先在slurm中开启DUUI-UIMA应用并告知用户端口号，用户可以通过login节点ssh -L 端口转发到自己的主机
2 For third-party use, the administrator should pre-enable the DUUI-UIMA application in slurm and inform the user of the port number, which the user can forward to their own host via the login node ssh -L port forwarding

必须保证slurmd(compute)节点内部有sif格式的DUUI-UIMA-Images

3 It must be ensured that the slurmd(compute) node has DUUI-UIMA-Images in sif format inside the node。

/data 文件夹是全节点共享的，root有权限读写

4 The /data folder is shared by all nodes, and root has permission to read and write to it.

--------

第一步：生成sif文件，用docker cp复制到计算节点内部 解压缩tar流  /data替代mpi 可以全局传输（docker java）

第二步：包装好sbatch命令 rest提交到节点 

第三步：remotedriver也可以直接访问

第四步： rest命令关闭sif image

第五步： 检查关闭的彻底不彻底



SlurmUtils:

```java
package org.texttechnologylab.DockerUnifiedUIMAInterface.driver.slurm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class SlurmUtils {
//    public static boolean isMasterNode(){
//        String s = whoIsMaster().split("=", 2)[1];
//        return s.equals(whoAmI());}
//
//    public static String  whoIsMaster(){
//        try {
//            InputStream pb = new ProcessBuilder("bash", "-c", "cat /etc/slurm/slurm.conf | grep ControlMachine").start().getInputStream();
//            BufferedReader reader = new BufferedReader(new InputStreamReader(pb));
//            return reader.readLine();
//
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
//    public static boolean submitHeldJob(String scriptPath) throws IOException, InterruptedException {
//        ProcessBuilder pb = new ProcessBuilder("sbatch", "--hold", scriptPath);
//        pb.redirectErrorStream(true);
//        Process process = pb.start();
//
//        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
//            String line;
//            while ((line = reader.readLine()) != null) {
//                if (line.startsWith("Submitted batch job")) {
//                    return true;
//                }
//            }
//        }
//
//        int exitCode = process.waitFor();
//        if (exitCode != 0) {
//            throw new RuntimeException("sbatch submission failed");
//        }
//        return false;
//    }


//    public static void releaseJob(String jobId) throws IOException, InterruptedException {
//        ProcessBuilder pb = new ProcessBuilder("scontrol", "release", jobId);
//        pb.environment().put("SLURM_JOB_ID", jobId);
//        pb.inheritIO();
//        Process process = pb.start();
//        int exitCode = process.waitFor();
//        if (exitCode != 0) {
//            throw new RuntimeException("Failed to release job " + jobId);
//        }
//    }


//    public static List<Integer> getHeldJobs() throws IOException, InterruptedException {
//        List<Integer> jobIds = new ArrayList<>();
//        ProcessBuilder pb = new ProcessBuilder("squeue", "--user=" + System.getProperty("user.name"), "--state=PD", "-h", "-o", "%i");
//        Process process = pb.start();
//
//        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
//            String line;
//            while ((line = reader.readLine()) != null) {
//                jobIds.add(Integer.parseInt(line.trim()));
//            }
//        }
//
//        int exitCode = process.waitFor();
//        if (exitCode != 0) {
//            throw new RuntimeException("Failed to fetch held jobs");
//        }
//
//        return jobIds;
//    }
//
//    public static void releaseAllHeldJobs() throws IOException, InterruptedException {
//        List<Integer> heldJobs = getHeldJobs();
//        for (int jobId : heldJobs) {
//            releaseJob(jobId);
//        }
//    }
//

//    public static void squeue() throws IOException {
//        ProcessBuilder pb = new ProcessBuilder("squeue");
//        pb.inheritIO();
//        pb.start();
//    }


//    public static void cancelJob(int jobId) throws IOException, InterruptedException {
//        ProcessBuilder pb = new ProcessBuilder("scancel", String.valueOf(jobId));
//        pb.inheritIO();
//        Process process = pb.start();
//        int exitCode = process.waitFor();
//        if (exitCode != 0) {
//            throw new RuntimeException("Failed to cancel job " + jobId);
//        }
//    }


//    public static boolean checkSocatInstalled(){
//        try {
//            InputStream pb = new ProcessBuilder("which", "socat").start().getInputStream();
//            BufferedReader reader = new BufferedReader(new InputStreamReader(pb));
//            String s = reader.readLine();
//            System.out.println(s);
//            return !s.isEmpty();
//
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
//    public static boolean checkSlurmInstalled(){
//        try {
//            InputStream pb = new ProcessBuilder("slurmd", " ", "-C").start().getInputStream();
//            BufferedReader reader = new BufferedReader(new InputStreamReader(pb));
//            String s = reader.readLine();
//            System.out.println(s);
//            return !s.isEmpty();
//
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }


    public static boolean isDockerImagePresent(String imageName) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("docker", "image", "inspect", imageName);
        Process start = pb.start();
        return start.waitFor() == 0;

    }


    public static String whoAmI() {
        try {
            InputStream pb = new ProcessBuilder("hostname").start().getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(pb));
            return reader.readLine();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean pullSifImagefromRemoteDockerRepo(String dockerImageName, String sifImagePath) {
        if (sifImagePath == null || sifImagePath.isEmpty()) {
            throw new IllegalArgumentException("imagePath is null or empty");
        }

        System.out.println("Correct format: apptainer build {Sif_image_name}.sif docker://{docker_repo/image:tag}");

        List<String> command = List.of(
                "apptainer", "build",
                sifImagePath,
                "docker://" + dockerImageName
        );
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        pb.inheritIO();
        try {
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to run apptainer build", e);
        }
    }


    public static boolean pullSifImagefromLocalDockerRepo(String dockerImageName, String sifImagePath) {
        if (dockerImageName == null || dockerImageName.isEmpty()) {
            throw new IllegalArgumentException("dockerImageName is null or empty");
        }

        System.out.println("Correct format: apptainer build {Sif_image_name}.sif docker-daemon://{image:tag}");

        List<String> command = List.of(
                "apptainer", "build",
                sifImagePath,
                "docker-daemon://" + dockerImageName
        );

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        pb.inheritIO();
        try {
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to run apptainer build", e);
        }
    }

    public static String submitJob(String jobName, String hostport,
                                   String cpus, String gpu, String entry, String error, String imageport, String mem,
                                   String time,
                                   String output, String sifname, String saveIn, String uvicorn
    ) throws IOException, InterruptedException {

        Path script = Paths.get("/tmp", jobName + ".sh");


        List<String> lines = List.of(
                "#!/bin/bash",
                "#SBATCH --job-name=" + jobName,
                "#SBATCH --cpus-per-task=" + cpus,
                "#SBATCH --time=" + time,
                "",
                "PORT=" + hostport,
                "UVI=\"" + uvicorn + "\"",               //
                "INNER=" + imageport,
                "IMG=\"" + saveIn + "\"",
                "INTOIMAGE=\"" + entry + "\"",           // looks like "cd /usr/src/app"
                "",
                "apptainer exec \"$IMG\" \\",
                "  sh -c \"$INTOIMAGE && $UVI --host 0.0.0.0 --port $INNER\" &",
                "PID=$!",
                "",
                "socat TCP-LISTEN:$PORT,reuseaddr,fork TCP:127.0.0.1:$INNER &",
                "PID_SOCAT=$!",
                "",
                "trap 'kill $PID $PID_SOCAT 2>/dev/null' EXIT",
                "",
                "wait $PID"
        );


        Files.write(script, lines, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        script.toFile().setExecutable(true);
        System.out.println("Slurm batch script written to: " + script);


        ProcessBuilder pb = new ProcessBuilder("sbatch", "--parsable", script.toString());
        pb.redirectErrorStream(true);
        Process proc = pb.start();

        String jobId;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            jobId = br.readLine();
        }
        int exit = proc.waitFor();
        if (exit != 0 || jobId == null || jobId.isBlank()) {
            throw new IllegalStateException("sbatch failed，exit=" + exit);
        }

        System.out.println("Job submitted. ID = " + jobId);
        return jobId.trim();
    }

}

```



1 检查主机名

2 远端拉取到本地位置

3 本地拉取到本地

-------

SlurmRestd

1容器名字id

2所有容器名字

3检查restd容器有没有启动

4列出hostname

5根据主机名字生成jwt token

6用root生成token

7在容器名执行

8根据restd查

9根据restd提交

--------------

composer 996 add driver: 放context 放driver

add()

###### composer 1607初始化流水线

```
private static String slurmJobName = "slurmJobName";  1
private static String slurmHostPort = "slurmHostPort"; 2
private static String slurmRuntime = "slurmRuntime";3 
private static String slurmCpus = "slurmCpus";4 
private static String slurmMemory = "slurmMemory";5
private static String slurmErrorLocation = "slurmErrorLocation";6
private static String slurmOutPutLocation = "slurmOutPutLocation";7
private static String slurmSIFLocation = "slurmSIFLocation";8
private static String slurmGPU = "slurmGPU";9
private static String slurmSIFImageName = "slurmSIFImageName";10
private static String slurmNoShutdown = "slurmNoShutdown";11
private static String slurmUvicorn = "slurmUvicorn";12
private static String slurmScript = "slurmScript";13
    private static String partition = "slurmPartition";
    private static String nodelist = "slurmNodelist";
    
```

```
//1
    public DUUIPipelineComponent withSlurmJobName(String jobName) {
        if (_finalizedEncoded != null) {
            throw new RuntimeException("DUUIPipelineComponent has already been finalized, it is immutable now!");
        }
        if (jobName == null) {
            _options.remove(slurmJobName);
            return this;
        }
        _options.put(slurmJobName, jobName);
        _options.put(componentName, jobName);
        return this;
    }
2
    public DUUIPipelineComponent withSlurmHostPort(String port) {
        if (_finalizedEncoded != null) {
            throw new RuntimeException("DUUIPipelineComponent has already been finalized, it is immutable now!");
        }
        if (port == null) {
            _options.remove(slurmHostPort);
            return this;
        }
        _options.put(slurmHostPort, port);
        return this;
    }
//3
    public DUUIPipelineComponent withSlurmRuntime(String time) {
        if (_finalizedEncoded != null) {
            throw new RuntimeException("DUUIPipelineComponent has already been finalized, it is immutable now!");
        }
        if (time == null) {
            _options.remove(slurmRuntime);
            return this;
        }
        _options.put(slurmRuntime, time);
        return this;
    }

//4
    public DUUIPipelineComponent withSlurmCpus(String num) {
        if (_finalizedEncoded != null) {
            throw new RuntimeException("DUUIPipelineComponent has already been finalized, it is immutable now!");
        }
        if (num == null) {
            _options.remove(slurmCpus);
            return this;
        }
        _options.put(slurmCpus, num);
        return this;
    }

//5
    public DUUIPipelineComponent withSlurmMemory(String numG) {
        if (_finalizedEncoded != null) {
            throw new RuntimeException("DUUIPipelineComponent has already been finalized, it is immutable now!");
        }
        if (numG == null) {
            _options.remove(slurmMemory);
            return this;
        }
        _options.put(slurmMemory, numG);
        return this;
    }
//7
    public DUUIPipelineComponent withSlurmOutPutLocation(String loc) {
        if (_finalizedEncoded != null) {
            throw new RuntimeException("DUUIPipelineComponent has already been finalized, it is immutable now!");
        }
        if (loc == null) {
            _options.remove(slurmOutPutLocation);
            return this;
        }
        _options.put(slurmOutPutLocation, loc);
        return this;
    }
    //6
    public DUUIPipelineComponent withSlurmErrorLocation(String loc) {
        if (_finalizedEncoded != null) {
            throw new RuntimeException("DUUIPipelineComponent has already been finalized, it is immutable now!");
        }
        if (loc == null) {
            _options.remove(slurmErrorLocation);
            return this;
        }
        _options.put(slurmErrorLocation, loc);
        return this;
    }
//8
    public DUUIPipelineComponent withSlurmSaveIn(String saveTo) {
        if (_finalizedEncoded != null) {
            throw new RuntimeException("DUUIPipelineComponent has already been finalized, it is immutable now!");
        }
        if (saveTo == null) {
            _options.remove(slurmSIFLocation);
            return this;
        }
        _options.put(slurmSIFLocation, saveTo);
        return this;
    }

//9
    public DUUIPipelineComponent withSlurmGPU(String num) {
        if (_finalizedEncoded != null) {
            throw new RuntimeException("DUUIPipelineComponent has already been finalized, it is immutable now!");
        }
        if (num == null) {
            _options.remove(slurmGPU);
            return this;
        }
        _options.put(slurmGPU, num);
        return this;
    }
//10
    public DUUIPipelineComponent withSlurmSIFName(String sifName) {
        if (_finalizedEncoded != null) {
            throw new RuntimeException("DUUIPipelineComponent has already been finalized, it is immutable now!");
        }
        if (sifName == null) {
            _options.remove(slurmSIFImageName);
            return this;
        }
        _options.put(slurmSIFImageName, sifName);
        return this;

    }

12
    public DUUIPipelineComponent withSlurmUvicorn(String loc) {
        if (_finalizedEncoded != null) {
            throw new RuntimeException("DUUIPipelineComponent has already been finalized, it is immutable now!");
        }
        if (loc == null) {
            _options.remove(slurmUvicorn);
            return this;
        }
        _options.put(slurmUvicorn, loc);
        return this;
    }

13
    public DUUIPipelineComponent withSlurmScript(String script) {
        if (_finalizedEncoded != null) {
            throw new RuntimeException("DUUIPipelineComponent has already been finalized, it is immutable now!");
        }
        if (script == null) {
            _options.remove(slurmScript);
            return this;
        }
        _options.put(slurmScript, script);
        return this;
    }


    public String getSlurmSIFImageName() {
        return _options.get(slurmSIFImageName);
    }

    public String getSlurmGPU() {
        return  _options.get(slurmGPU);
    }

    public String getSlurmSIFLocation() {
        return  _options.get(slurmSIFLocation);
    }

    public String getSlurmOutPutLocation() {
        return  _options.get(slurmOutPutLocation);
    }

    public String getSlurmMem() {
        return  _options.get(slurmMemory);
    }

    public String getSlurmCpus() {
        return  _options.get(slurmCpus);
    }

    public String getSlurmRuntime() {
        return  _options.get(slurmRuntime);
    }

    public String getSlurmHostPort() {
        return  _options.get(slurmHostPort);
    }
    public String getSlurmJobName() {
        return  _options.get(slurmJobName);
    }
    public String getSlurmErrorLocation() {
        return  _options.get(slurmErrorLocation);
    }

    public Boolean getSlurmRunAfterExit(Boolean defaultValue) {
        String result = _options.get(slurmNoShutdown);
        if (result == null) return defaultValue;
        return Boolean.parseBoolean(result);
    }

    public String getSlurmUvicorn() {
        return  _options.get(slurmUvicorn);
    }

    public String getSlurmScript(){
        return  _options.get(slurmScript);
    }
}
```





docker driver:

uuid记录一种组件 一种image 对应多个实例 

!!!!!

每次删掉一个uuid对应的端口 就可以放回池子复用

