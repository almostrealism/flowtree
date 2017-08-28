import urllib2
import os
import pdb


# from python_helpers import instance
home = '/root'

# -----------cron
# Download the heart_beat crontab
response = urllib2.urlopen('http://msched.us-west-2.elasticbeanstalk.com/crontab')
infile = response.read()
dir = '/etc/cron.d/'
os.chdir(dir)
with open('heart_beat', 'w') as outfile:
    outfile.write(infile)

# Start cron
os.system('/etc/init.d/crond start')

# -----------ssh
# Download the git credentials and add them to ssh-agent

if not os.path.exists(home + '/.ssh'):
    # This is sort of redundent as we assume ssh is installed in which case this dir exists
    os.system('mkdir ' + home + '/.ssh')

os.chdir(home + '/.ssh')

for p in ['prv', 'pub']:
    response = urllib2.urlopen('http://msched.us-west-2.elasticbeanstalk.com/'+p+'_git_key')
    infile = response.read()

    with open(p+'_git_key', 'w') as outfile:
        outfile.write(infile)

os.system('mv '+home+'/.ssh/pub_git_key '+home+'/.ssh/git_gen.pub')
os.system('mv '+home+'/.ssh/prv_git_key '+home+'/.ssh/git_gen')

# Add ssh keys

bash_cmd="""
eval `ssh-agent -s`;
chmod 400 """+home+"""/.ssh/git_gen;
ssh-add """+home+"""/.ssh/git_gen;
cat > """+home+"""/.ssh/config<<-EOM
Host bitbucket.org
 IdentityFile """+home+"""/.ssh/git_gen
"""
os.system(bash_cmd)
os.system('chmod 600 '+home+'/.ssh/config;')
# -----------git
# Sync repos
os.system('cd '+home)
os.system('git pull git@bitbucket.org:terraai/msched.git/')

# --------------- airflow.cfg
# Pull airflow.cfg
# afc = urllib.URLopener()
# afc.retrieve('http://msched.us-west-2.elasticbeanstalk.com/AirflowConfig', '~/airflow/airflow.cfg')
# os.system()
# --------------- TEMP
# move airflow.cfg to /airflow
# os.system('rm /airflow/airflow.cfg')

os.system('airflow initdb')

os.system('java -cp /root/FlowTree-0.1-rc.jar:/root/TreeView-0.1-rc.jar:/root/Common-0.1-rc.jar:/root/hsqldb-2.3.4.jar:/root/jsch-0.1.53.jar org.almostrealism.flow.Server /root/flowtree.conf -p &')
