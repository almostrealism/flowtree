# Start with centos 7
FROM centos:7

# Set the working directory to /app
WORKDIR /root

# Copy the current directory contents into the container at /app
ADD . /root

RUN rm /etc/yum/pluginconf.d/fastestmirror.conf

RUN yum update
RUN yum install -y sudo

# Install wget
RUN sudo yum install -y wget

# Install Almost Realism FlowTree for parallel processing
RUN wget https://bitbucket.org/ashesfall/flowtree/downloads/FlowTree-0.1-rc.jar
RUN wget https://bitbucket.org/ashesfall/flowtree/downloads/TreeView-0.1-rc.jar
RUN wget https://bitbucket.org/ashesfall/flowtree/downloads/hsqldb-2.3.4.jar
RUN wget https://bitbucket.org/ashesfall/flowtree/downloads/jsch-0.1.53.jar

# Install python
RUN sudo yum install -y centos-release-scl
RUN sudo yum install -y python27

# Install pip and python-devel
RUN curl "https://bootstrap.pypa.io/get-pip.py" -o "get-pip.py"
RUN sudo python get-pip.py
RUN sudo yum install -y python-devel

# Install gcc
RUN sudo yum install -y gcc

# Download and install aws log agent
# RUN sudo yum install -y awslogs
# RUN curl https://s3.amazonaws.com/aws-cloudwatch/downloads/latest/awslogs-agent-setup.py -O
# RUN sudo python ./awslogs-agent-setup.py --region us-west-2
# RUN sudo pip3.5 install awscli-cwlogs. 
RUN sudo curl https://s3.amazonaws.com//aws-cloudwatch/downloads/latest/awslogs-agent-setup.py -O
RUN sudo curl https://s3.amazonaws.com//aws-cloudwatch/downloads/latest/AgentDependencies.tar.gz -O
RUN sudo tar xvf AgentDependencies.tar.gz -C /tmp/

RUN pip install --upgrade
RUN pip install -U pip setuptools

RUN pip install -r requirements.txt
# RUN sudo yum install -y build-essential
RUN sudo yum install -y nano
# RUN sudo yum install -y vim-tiny
RUN sudo yum install -y git

# Install cron
RUN yum install -y cronie

# Run app.py when the container launches
CMD ["python", "init.py"]