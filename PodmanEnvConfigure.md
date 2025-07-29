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

3.  check nvidia-ctk cdi list, if `NFO[0000] Found 0 CDI devices` then goto 4 else goto 5

4.  `sudo nvidia-ctk cdi generate --output=/etc/cdi/nvidia.yaml`

5. `nvidia-ctk cdi list` should have 

   NFO[0000] Found X CDI devices
   nvidia.com/gpu=0
   nvidia.com/gpu=XXXXXX
   nvidia.com/gpu=all

6.  `podman run --rm --device nvidia.com/gpu=all nvidia/cuda:12.9.0-base-ubuntu22.04 nvidia-smi` should == `nvidia-smi`