FROM openjdk:8-jdk-alpine

# Install required packages for installation
RUN apk add --no-cache \
    bash \
    su-exec \
    python \
    git \
    maven



# Downloading maven in local bin archive
RUN cd /usr/local
ADD https://www-eu.apache.org/dist/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz .
RUN ln -s apache-maven-3.6.3 apache-maven


COPY apache-maven.sh /etc/profile.d/
RUN source /etc/profile.d/apache-maven.sh


RUN git clone https://git-wip-us.apache.org/repos/asf/atlas.git atlas \
    && cd atlas \
    && git checkout branch-2.0 \
    && export MAVEN_OPTS="-Xms2g -Xmx2g"

ADD https://github.com/manjitsin/apache-atlas-setup/releases/download/2.0/apache-atlas-2.1.0-SNAPSHOT-server.tar.gz /

# Unarchive
RUN set -x \
    && cd / \
    && tar -xvzf apache-atlas-2.1.0-SNAPSHOT-server.tar.gz \
    && rm apache-atlas-2.1.0-SNAPSHOT-server.tar.gz


WORKDIR /apache-atlas-2.1.0-SNAPSHOT

EXPOSE 21000

ENV PATH=$PATH:/apache-atlas-2.1.0-SNAPSHOT/bin

CMD ["/bin/bash", "-c", "/apache-atlas-2.1.0-SNAPSHOT/bin/atlas_start.py; tail -fF /apache-atlas-2.1.0-SNAPSHOT/logs/application.log"]


