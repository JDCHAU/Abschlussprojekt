# Login-Node-Image

## use ubuntu compile slurm deb packages with version 24.11.5

Steps on your host machine

```shell
sudo apt-get update
sudo apt-get install -y build-essential devscripts debhelper fakeroot

dpkg-buildpackage -b -us -uc
```

**if error:** 

```shell
dpkg-checkbuilddeps: Error: Unmet build dependencies: 
  debhelper (>= 12)
  dh-exec
  hdf5-helpers
  libcurl4-openssl-dev
  libdbus-1-dev
  libfreeipmi-dev
  libgtk2.0-dev
  libhdf5-dev
  libhttp-parser-dev
  libhwloc-dev
  libipmimonitoring-dev
  libjson-c-dev
  libjwt-dev
  liblua5.3-dev
  liblz4-dev
  libmariadb-dev | libmysqlclient-dev
  libmunge-dev
  libpam0g-dev
  libperl-dev
  libpmix-dev
  librdkafka-dev
  librrd-dev
  libyaml-dev
  man2html-base
  po-debconf
  
```

**then:**

```shell
sudo apt-get update
sudo apt-get install -y \
  debhelper dh-exec \
  hdf5-helpers libcurl4-openssl-dev libdbus-1-dev libfreeipmi-dev \
  libgtk2.0-dev libhdf5-dev libhttp-parser-dev libhwloc-dev \
  libipmimonitoring-dev libjson-c-dev libjwt-dev liblua5.3-dev \
  liblz4-dev libmariadb-dev libmunge-dev libpam0g-dev libperl-dev \
  libpmix-dev librdkafka-dev librrd-dev libyaml-dev \
  man2html-base po-debconf

```

Under GUI System give .dirs .installs files +x permissions, must remove them.(Please notice if your file system is NTFS exFAT, chmod -x does not work as well, only ext4 is supported)

To prove: 

find debian -type f -name "*.dirs"

find debian -type f -name "*.install"

DO:

find debian -type f -name "*.\<every type contains error\>" -exec chmod -x {} \; 

--------------

```docker build -t test .```

```docker run -it test```

```root@xxxx:/packages# slurmd --version ```

if see slurm 24.11.5 then a basic image is done!

--------

# Login-Node

### why ubuntu not RHEL?

**In containerized environments, RHEL systems force the use of the PAM module, so this can create a lot of complications, even the inability to use the passwd command.**

Existing sshd servers like https://hub.docker.com/r/linuxserver/openssh-server 
use the alpine system, which is also not very compatible with slurm.
So using ubuntu/debian becomes a compromise. 

Note that in rocky systems and ubuntu the same services have different uid's, such as munge. So we specify that the munge service occupies port **998**.

---

The minimum configuration required for the login node is ***(all of configurations is already in entrypoint.sh, here is just to show)***:

1. munged ` gosu munge /usr/sbin/munged --foreground`                                                                                                                                          

2. check sshd and add a non-root user:

   if you want su, don't forget `passwd`  set a password for your root user.

```shell
[root@login /]# which sshd
/usr/sbin/sshd
[root@login /]# useradd -m student
[root@login /]# echo "student:student" | chpasswd
[root@login /]# su student
[student@login /]$ sinfo
PARTITION AVAIL  TIMELIMIT  NODES  STATE NODELIST
normal*      up 5-00:00:00      1   idle compute1

```

3. check ssh config files, in ubuntu after install sshd no need manual run ssh-keygen -A

```shell
ls /etc/ssh

root@login:/etc/ssh# ls
moduli        ssh_host_ecdsa_key      ssh_host_ed25519_key.pub  sshd_config
ssh_config    ssh_host_ecdsa_key.pub  ssh_host_rsa_key          sshd_config.d
ssh_config.d  ssh_host_ed25519_key    ssh_host_rsa_key.pub

```

4.  modify /etc/ssh/sshd_config

```shell
# This is the sshd server system-wide configuration file.  See
# sshd_config(5) for more information.

# This sshd was compiled with PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/games

# The strategy used for options in the default sshd_config shipped with
# OpenSSH is to specify options with their default value where
# possible, but leave them commented.  Uncommented options override the
# default value.

Include /etc/ssh/sshd_config.d/*.conf

# When systemd socket activation is used (the default), the socket
# configuration must be re-generated after changing Port, AddressFamily, or
# ListenAddress.
#
# For changes to take effect, run:
#
#   systemctl daemon-reload
#   systemctl restart ssh.socket
#
Port 22
#AddressFamily any
#ListenAddress 0.0.0.0
#ListenAddress ::

#HostKey /etc/ssh/ssh_host_rsa_key
#HostKey /etc/ssh/ssh_host_ecdsa_key
#HostKey /etc/ssh/ssh_host_ed25519_key

# Ciphers and keying
#RekeyLimit default none

# Logging
#SyslogFacility AUTH
LogLevel INFO

# Authentication:

#LoginGraceTime 2m
PermitRootLogin prohibit-password
#StrictModes yes
#MaxAuthTries 6
#MaxSessions 10

#PubkeyAuthentication yes

# Expect .ssh/authorized_keys2 to be disregarded by default in future.
#AuthorizedKeysFile	.ssh/authorized_keys .ssh/authorized_keys2

#AuthorizedPrincipalsFile none

#AuthorizedKeysCommand none
#AuthorizedKeysCommandUser nobody

# For this to work you will also need host keys in /etc/ssh/ssh_known_hosts
#HostbasedAuthentication no
# Change to yes if you don't trust ~/.ssh/known_hosts for
# HostbasedAuthentication
#IgnoreUserKnownHosts no
# Don't read the user's ~/.rhosts and ~/.shosts files
#IgnoreRhosts yes

# To disable tunneled clear text passwords, change to no here!
PasswordAuthentication yes
#PermitEmptyPasswords no

# Change to yes to enable challenge-response passwords (beware issues with
# some PAM modules and threads)
KbdInteractiveAuthentication no

# Kerberos options
#KerberosAuthentication no
#KerberosOrLocalPasswd yes
#KerberosTicketCleanup yes
#KerberosGetAFSToken no

# GSSAPI options
#GSSAPIAuthentication no
#GSSAPICleanupCredentials yes
#GSSAPIStrictAcceptorCheck yes
#GSSAPIKeyExchange no

# Set this to 'yes' to enable PAM authentication, account processing,
# and session processing. If this is enabled, PAM authentication will
# be allowed through the KbdInteractiveAuthentication and
# PasswordAuthentication.  Depending on your PAM configuration,
# PAM authentication via KbdInteractiveAuthentication may bypass
# the setting of "PermitRootLogin prohibit-password".
# If you just want the PAM account and session checks to run without
# PAM authentication, then enable this but set PasswordAuthentication
# and KbdInteractiveAuthentication to 'no'.
UsePAM no

#AllowAgentForwarding yes
#AllowTcpForwarding yes
#GatewayPorts no
X11Forwarding yes
#X11DisplayOffset 10
#X11UseLocalhost yes
#PermitTTY yes
PrintMotd no
#PrintLastLog yes
#TCPKeepAlive yes
#PermitUserEnvironment no
#Compression delayed
#ClientAliveInterval 0
#ClientAliveCountMax 3
#UseDNS no
#PidFile /run/sshd.pid
#MaxStartups 10:30:100
#PermitTunnel no
#ChrootDirectory none
#VersionAddendum none

# no default banner path
#Banner none

# Allow client to pass locale environment variables
AcceptEnv LANG LC_*

# override default of no subsystems
Subsystem	sftp	/usr/lib/openssh/sftp-server

# Example of overriding settings on a per-user basis
#Match User anoncvs
#	X11Forwarding no
#	AllowTcpForwarding no
#	PermitTTY no
#	ForceCommand cvs server
AllowUsers student
```

## use ssh connect to port 2222

```shell
# cover old connecttion history
ssh-keygen -f ~/.ssh/known_hosts -R '[localhost]:2222'
ssh student@localhost -p 2222
```

