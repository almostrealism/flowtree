FROM centos:centos6

# Set the working directory to /app
WORKDIR /root

# Copy the current directory contents into the container at /app
ADD . /root

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
RUN sudo yum install -y sudo

# Install wget
RUN sudo yum install -y wget

# Install pip and python-devel
RUN curl "https://bootstrap.pypa.io/get-pip.py" -o "get-pip.py"
RUN sudo python get-pip.py
RUN sudo yum install -y python-devel
RUN sudo pip install --upgrade setuptools

# Install gcc
RUN sudo yum install -y gcc

# Install cron
RUN yum install -y cronie

# Install Almost Realism FlowTree for parallel processing
RUN wget https://bitbucket.org/ashesfall/flowtree/downloads/FlowTree-0.1-rc.jar
RUN wget https://bitbucket.org/ashesfall/flowtree/downloads/TreeView-0.1-rc.jar
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
RUN sudo python ./awslogs-agent-setup.py -n --region us-west-2 --dependency-path /tmp/AgentDependencies -c awslogs-agent.conf

RUN pip install --upgrade
RUN pip install -U pip setuptools

RUN pip install -r requirements.txt
# RUN pip install -y airflow[slack, s3, rabbitmq, postgres, celery]
RUN sudo yum install -y nano
RUN sudo yum install -y git

# Run app.py when the container launches
CMD ["python", "init.py"]