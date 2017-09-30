FROM centos:centos6

# Set the working directory to /app
WORKDIR /root

# Copy the current directory contents into the container at /app
ADD . /root

# Add airflow home for install
ENV AIRFLOW_HOME=/airflow

EXPOSE 8080
EXPOSE 7766
EXPOSE 11211
EXPOSE 8161
EXPOSE 61616

# Install yum dependencies
RUN yum -y update && \
    yum groupinstall -y development && \
    yum install -y \
    bzip2-devel \
    git \
    hostname \
    openssl \
    openssl-devel \
    sqlite-devel \
    sudo \
    tar \
    wget \
    zlib-dev

# Install python2.7
RUN cd /tmp && \
    wget https://www.python.org/ftp/python/2.7.8/Python-2.7.8.tgz && \
    tar xvfz Python-2.7.8.tgz && \
    cd Python-2.7.8 && \
    ./configure --prefix=/usr/local && \
    make && \
    make altinstall

RUN yum install -y sudo

RUN sudo yum update -y
RUN sudo yum install -y wget

# Install pip and python-devel
RUN curl "https://bootstrap.pypa.io/get-pip.py" -o "get-pip.py"
RUN sudo python get-pip.py
RUN sudo yum install -y python-devel
RUN sudo pip install --upgrade setuptools

# Install gcc and cron
RUN sudo yum install -y gcc
RUN yum install -y cronie

# Install open JDK
RUN su -c "yum install -y java-1.8.0-openjdk"

# Install Almost Realism FlowTree for parallel processing
RUN wget https://bitbucket.org/ashesfall/flowtree/downloads/FlowTree-0.1-rc.jar
RUN wget https://bitbucket.org/ashesfall/flowtree/downloads/TreeView-0.1-rc.jar
RUN wget https://bitbucket.org/ashesfall/flowtree/downloads/Common-0.1-rc.jar
RUN wget https://bitbucket.org/ashesfall/flowtree/downloads/hsqldb-2.3.4.jar
RUN wget https://bitbucket.org/ashesfall/flowtree/downloads/jsch-0.1.53.jar

# Download and install aws log agent
# RUN sudo yum install -y awslogs
# RUN curl https://s3.amazonaws.com/aws-cloudwatch/downloads/latest/awslogs-agent-setup.py -O
# RUN sudo python ./awslogs-agent-setup.py --region us-west-2
# RUN sudo pip3.5 install awscli-cwlogs. 
RUN sudo curl https://s3.amazonaws.com//aws-cloudwatch/downloads/latest/awslogs-agent-setup.py -O
RUN sudo curl https://s3.amazonaws.com//aws-cloudwatch/downloads/latest/AgentDependencies.tar.gz -O
RUN sudo tar xvf AgentDependencies.tar.gz -C /tmp/

# Install Cloud Watch
RUN sudo touch /var/log/awslogs-agent-setup.log
RUN ls /var/log
# RUN sudo python ./awslogs-agent-setup.py -n --region us-west-2 --dependency-path /tmp/AgentDependencies -c awslogs-agent.conf

RUN sudo yum install -y epel-release
RUN sudo yum install -y nfs-utils
RUN sudo yum install -y which
RUN sudo yum install -y nano
RUN sudo yum install -y git

RUN sudo yum install -y postgresql-server postgresql-contrib
RUN sudo mkdir /home/postgres ; sudo chown postgres /home/postgres
RUN sudo su postgres -c "mkdir /home/postgres/data"
RUN export PGDATA=/home/postgres/data ; su postgres -c "pg_ctl start -l /home/postgres/pglog"

RUN export PATH=/usr/local/bin:$PATH
RUN sudo mv /usr/bin/python /usr/bin/python-old
RUN sudo ln -s /usr/local/bin/python2.7 /usr/bin/python

RUN curl "https://bootstrap.pypa.io/get-pip.py" -o "get-pip.py"
RUN sudo python get-pip.py

RUN pip install --upgrade setuptools
RUN pip install ez_setup
RUN which python
RUN python --version
RUN pip install unroll
RUN easy_install -U setuptools
RUN pip install unroll

# Install airflow
RUN pip install airflow
RUN pip install celery
RUN pip install airflow[slack]
RUN pip install airflow[s3]
RUN pip install airflow[rabbitmq]
RUN pip install airflow[postgres]

RUN mkdir /usr/local/lib/python2.7/site-packages/airflow/plugins
RUN cp executor/flowtree_executor.py /usr/local/lib/python2.7/site-packages/airflow/plugins/flowtree_executor.py

RUN pip install --upgrade
RUN pip install -U pip setuptools

RUN pip install -r requirements.txt

RUN tar -zxvf apache-activemq-5.15.0-bin.tar.gz

RUN tar -zxvf libevent-2.1.8-stable.tar.gz
RUN cd /root/libevent-2.1.8-stable ; chmod +x ./configure ; ./configure --prefix /usr/lib ; make ; sudo make install
RUN cd /root ; tar -zxvf memcached-1.5.1.tar.gz
RUN cd memcached-1.5.1 ; chmod +x ./configure ; ./configure --with-libevent=/usr/lib && make && sudo make install
RUN memcached&

# Run init.py when the container launches
CMD ["python", "init.py"]