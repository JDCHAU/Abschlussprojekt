# Permission

If you encounter permission-related issues, here is a possible solution：

Since the server environment may override the permissions preset in the dockerfile, most problems will occur in all files that specify a user. 

For Example,  Codes in Dockerfile:

```sh
RUN set -x \
    && chown $USER:$USER /etc/slurm/slurmdbd.conf \
    && chmod 600 /etc/slurm/slurmdbd.conf

RUN head -c32 /dev/urandom | base64 > /etc/slurm/jwt_hmac.key \
    && chown slurm:restd /etc/slurm/jwt_hmac.key \
    && chmod 640 /etc/slurm/jwt_hmac.key \
    && chmod 644 /etc/slurm/slurm.conf
```

1. You need to use podman inspect \<component name\>.

2. find mount-point e.g. :

   "Source": "/xx/yy/.../volumes/\<project name\>/_data"

3. if in rootless mode use **podman unshare**, then **chmod chown** again.

   

    

# GPU-Support

Podman also supports GPU usage in containers.

1. check nvidia-smi

2. check nvidia-container-tookit [NVIDIA Container Toolkit](https://docs.nvidia.com/datacenter/cloud-native/container-toolkit/install-guide.html)

3. check nvidia-ctk cdi list, if `NFO[0000] Found 0 CDI devices` then goto 4 else goto 5

4. `sudo nvidia-ctk cdi generate --output=/etc/cdi/nvidia.yaml`

5. `nvidia-ctk cdi list` should have 

   NFO[0000] Found X CDI devices
   nvidia.com/gpu=0
   nvidia.com/gpu=XXXXXX
   nvidia.com/gpu=all

6. `podman run --rm --device nvidia.com/gpu=all nvidia/cuda:12.9.0-base-ubuntu22.04 nvidia-smi` should == `nvidia-smi`



new Podman-compose Template

```
########################
# SLURM Podman CLUSTER #
########################

## VOLUMES
volumes:
  etc_munge:
  etc_slurm:
  slurm_jobdir:
  var_lib_mysql:
  var_log_slurm:
  entry:
  grafana_lib:
  es_logs:
## SERVICES
services:
  # traefik:
  #   image: traefik:2.11
  #   container_name: traefik
  #   restart: unless-stopped
  #   command:
  #     - --providers.docker=true
  #     - --providers.docker.exposedbydefault=false
  #     - --entrypoints.web.address=:80
  #     - --api.dashboard=true
  #     - --api.insecure=true
  #     - --log.level=INFO
    # ports:
    #   - "80:80"
    #   - "8080:8080"
    # volumes:
    #   - /var/run/docker.sock:/var/run/docker.sock:ro
    # networks:
    #   - slurm_net


  slurm-base:
    image: slurm-docker-cluster:24-11-5_GPU
    build:
      context: .
      dockerfile: Dockerfile 
      args:
        SLURM_TAG: slurm-24-11-5   
    volumes:
      - var_lib_mysql:/var/lib/mysql
    environment:
      - TZ=Europe/Berlin
    networks:
      - slurm_net      

  # MariaDB
  mysql:
    image: mariadb:11
    hostname: mysql
    container_name: mysql
    environment:
      MYSQL_ROOT_PASSWORD: testtest
      MYSQL_DATABASE: slurm_acct_db
      MYSQL_USER: slurm
      MYSQL_PASSWORD: password
    volumes:
      - var_lib_mysql:/var/lib/mysql
    networks:
      - slurm_net


  # SlurmDBd
  slurmdbd:
    image: slurm-docker-cluster:24-11-5_GPU
    command: ["slurmdbd"]
    container_name: slurmdbd
    hostname: slurmdbd
    environment: 
      PUID: 990
      GUID: 990
      TZ: Europe/Berlin
    volumes:
      - etc_munge:/etc/munge
      - etc_slurm:/etc/slurm
      - var_log_slurm:/var/log/slurm
      - entry:/usr/local/bin
      #- /sys/fs/cgroup:/sys/fs/cgroup:ro  
    expose:
      - "6819"
    depends_on:
      - mysql
    networks:
      - slurm_net


  # Slurmctld：
  slurmctld:
    image: slurm-docker-cluster:24-11-5_GPU
    runtime: nvidia
    command: ["slurmctld"]
    container_name: slurmctld
    hostname: slurmctld
    environment: 
      PUID: 990
      GUID: 990
      TZ: Europe/Berlin
    volumes:
      - etc_munge:/etc/munge
      - etc_slurm:/etc/slurm
      - slurm_jobdir:/data
      - var_log_slurm:/var/log/slurm
      - entry:/usr/local/bin
      #- /sys/fs/cgroup:/sys/fs/cgroup:ro  

    devices:
      - "nvidia.com/gpu=all"

    ports:
    - "16817:6817"
    - "6820"
    depends_on:
      - "slurmdbd"
      - "elasticsearch"
      
    networks:
      - slurm_net
    

  # Compute node 1
  compute1:
    
    image: slurm-docker-cluster:24-11-5_GPU
      #privileged: true
    #runtime: nvidia
    command: ["slurmd"]
    hostname: compute1
    container_name: compute1
    environment: 
      PUID: 990
      GUID: 990
      TZ: Europe/Berlin
        #NVIDIA_VISIBLE_DEVICES: all
    devices:
        - "nvidia.com/gpu=all"    
        - "/dev/fuse:/dev/fuse"
      #- /dev/fuse
    cap_add:
      - ALL   
    security_opt:
        - seccomp=unconfined
        - apparmor:unconfined   
        - unmask=all 
        - label=disable
      
      
    volumes:
      - etc_munge:/etc/munge
      - etc_slurm:/etc/slurm
      - slurm_jobdir:/data
      - var_log_slurm:/var/log/slurm
      - entry:/usr/local/bin
      - ./sif_pool:/sif_pool:rw
      #- /sys/fs/cgroup:/sys/fs/cgroup:rw 

    #pid: host
    #cgroup: host 
    ports:
      - "6818"
      - "20000-20100:20000-20100"

    depends_on:
      - "slurmctld"
     
    networks:
      - slurm_net

  # # Compute node 2
  # compute2:
  #   image: slurm-docker-cluster:slurm-docker-cluster:24-11-5_GPU
  #   privileged: true
  #   command: ["slurmd"]
  #   hostname: compute2
  #   container_name: compute2
  #   environment: 
  #     PUID: 990
  #     GUID: 990
  #   volumes:
  #     - etc_munge:/etc/munge
  #     - etc_slurm:/etc/slurm
  #     - slurm_jobdir:/data
  #     - var_log_slurm:/var/log/slurm
  #   expose:
  #     - "6818"
  #   depends_on:
  #     - "slurmctld"

  slurmrestd:
    image: slurm-docker-cluster:24-11-5_GPU
    container_name: slurmrestd
    privileged: true
    command: ["slurmrestd"]
    hostname: slurmrestd
    depends_on:
      - slurmctld
    ports:
      - "6820:6820"      
    volumes:
      - etc_munge:/etc/munge
      - etc_slurm:/etc/slurm
      - entry:/usr/local/bin
      #- /sys/fs/cgroup:/sys/fs/cgroup:ro
    environment: 
      TZ: Europe/Berlin
    networks:
      - slurm_net

  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    hostname: grafana
    ports:
       - "3000:3000"
    depends_on: ["mysql", "slurmdbd", "slurmrestd" ]
    environment:
      - GF_SECURITY_ADMIN_USER=admin
      - GF_SECURITY_ADMIN_PASSWORD=admin
      - TZ=Europe/Berlin
    volumes: 
      - grafana_lib:/var/lib/grafana
    networks:
      - slurm_net
    
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:9.0.2
    container_name: elasticsearch
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - ES_JAVA_OPTS=-Xms512m -Xmx512m
      - TZ=Europe/Berlin
    ports: 
      - "9200:9200"

    ulimits: 
      memlock: {soft: -1, hard: -1}

    volumes: 
      - es_logs:/var/log/slurm/elasticsearch

    networks:
      - slurm_net

  login:
    image: login_server:latest
    container_name: login
    hostname: login             
    # stdin_open: true
    # tty: true
    privileged: true            
    #environment:
      # PUID: 990
      # GUID: 990
    ports:
      - "2222:22"
    volumes:
      - etc_munge:/etc/munge
      - etc_slurm:/etc/slurm
      - slurm_jobdir:/data
      - entry:/usr/local/bin
      - ./sif_pool:/sif_pool:rw
    depends_on:
      - slurmctld
    environment: 
      - TZ=Europe/Berlin  
    networks:
      - slurm_net

networks:
  slurm_net:
    name: slurm_net
    driver: bridge
```





