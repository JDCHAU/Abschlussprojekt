# Slurm-Driver

driver

- slurm	

  - slurmInDocker

    ​	-  SlurmRest.class

  - DUUISlurmDriver.class

  - DUUISlurmInterface.class

  - PortManager.class

  - SlurmUtils.class

  

------

slurmInDocker 

​	It is specially prepared for slurm to run in the docker environment, and it provides methods to interact with slurmrestd.

​	For example, it provides functions such as query, submission, cancellation, etc.



DUUISlurmDriver.class

​	Main logical controller for interaction with DUUI-Composer and slurm



 PortManager.class SlurmUtils.class

​	Functional implementation for slurmdriver's services, generating shell scripts, assigning ports, port mapping maintenance, 	fast api service probing ...

---------

Caveats:

1. The sif format image must be pre-positioned in the compute node, default sif dir is /sif_pool. default workdir is /data. SlurmUtils provides a way to convert a docker image to a sif.
2. The number of ports exposed in compose.yml is the same as the number of ports in PortManager, and defaults is 20000-20100. 