#!/bin/bash
set -e

mkdir -p /run/munge 
chown munge:munge /run/munge
chmod 755 /run/munge 
mkdir -p /var/log/munge 
chown munge:munge /var/log/munge 
chmod 700 /var/log/munge 
gosu munge /usr/sbin/munged --foreground &
useradd -m -s /bin/bash student
echo 'student:student' | chpasswd
echo "AllowUsers student" >> /etc/ssh/sshd_config
echo "UsePAM no" >> /etc/ssh/sshd_config
mkdir -p /run/sshd
exec /usr/sbin/sshd -D