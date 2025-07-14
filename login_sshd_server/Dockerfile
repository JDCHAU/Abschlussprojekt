FROM ubuntu:24.04
LABEL maintainer="Jiadong Zhou"

ENV TINI_VERSION=v0.19.0

RUN apt-get update && \
    apt-get install -y curl && \
    curl -fsSL https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini -o /usr/bin/tini && \
    chmod +x /usr/bin/tini


ARG USER=slurm
ARG PUID=990
ARG PGID=990 
ARG GOSU_VERSION=1.17
ARG SLURM_TAG=slurm-24-11-5

WORKDIR /packages
ADD slurm-deb/ /packages

ARG DEBIAN_FRONTEND=noninteractive

RUN apt-get update && \
    apt-get install -y software-properties-common && \
    add-apt-repository -y universe && \
    add-apt-repository -y multiverse && \
    apt-get install -y --no-install-recommends \
        build-essential \
        ca-certificates \
        curl wget \
        sudo \
        tree \
        munge libmunge-dev \
        openssh-server openssh-client \
        net-tools \
        unzip \
        python3 \
        vim && \
    apt-get install -y ./*.deb && \
    rm -rf /var/lib/apt/lists/*
    
# Download and install gosu
RUN set -ex \
    && wget -O /usr/local/bin/gosu "https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-amd64" \
    && wget -O /usr/local/bin/gosu.asc "https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-amd64.asc" \
    && export GNUPGHOME="$(mktemp -d)" \
    && gpg --batch --keyserver hkps://keys.openpgp.org --recv-keys B42F6819007F00F88E364FD4036A9C25BF357DD4 \
    && gpg --batch --verify /usr/local/bin/gosu.asc /usr/local/bin/gosu \
    && rm -rf "${GNUPGHOME}" /usr/local/bin/gosu.asc \
    && chmod +x /usr/local/bin/gosu \
    && gosu nobody true

 RUN userdel systemd-network 
RUN groupmod -g 998 munge &&\
    usermod -u 998 -g 998 munge &&\
    mkdir -p /var/lib/munge /var/log/munge /etc/munge&&\
    chmod 700 /var/lib/munge /var/log/munge &&\
    chown -R munge:munge /etc/munge /var/lib/munge /var/log/munge


RUN groupadd -r --gid=$PGID $USER &&\
    useradd -r -g $USER --uid=$PUID $USER 

# Copy configuration files
COPY conf_files/etc/slurm/slurm.conf /etc/slurm/slurm.conf
COPY conf_files/etc/slurm/slurmdbd.conf /etc/slurm/slurmdbd.conf
COPY conf_files/etc/slurm/cgroup.conf /etc/slurm/cgroup.conf
COPY conf_files/etc/slurm/gres.conf /etc/slurm/gres.conf    


RUN mkdir -p /run/munge &&\
    chown munge:munge /run/munge &&\
    chmod 755 /run/munge &&\
    mkdir -p /var/log/munge &&\
    chown munge:munge /var/log/munge &&\
    chmod 700 /var/log/munge 

COPY /entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
