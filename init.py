import urllib2
import os


# from python_helpers import instance
home = '/root'

# -----------cron
# Download the heart_beat crontab
# response = urllib2.urlopen('http://msched.us-west-2.elasticbeanstalk.com/crontab')
# infile = response.read()
dir = '/etc/cron.d/'
os.chdir(dir)
# with open('heart_beat', 'w') as outfile:
#     outfile.write(infile)

# Start cron
os.system('/etc/init.d/crond start')

# --------------- airflow.cfg
# Pull airflow.cfg
# afc = urllib.URLopener()
# afc.retrieve('http://ms   ched.us-west-2.elasticbeanstalk.com/AirflowConfig', '~/airflow/airflow.cfg')
# os.system()
# --------------- TEMP
# move airflow.cfg to /airflow
# os.system('rm /airflow/airflow.cfg')

os.system('sudo systemctl start postgresql')
os.system('java -cp /root/FlowTree-0.1-rc.jar:/root/TreeView-0.1-rc.jar:/root/Common-0.1-rc.jar:/root/hsqldb-2.3.4.jar:/root/jsch-0.1.53.jar org.almostrealism.flow.Server /root/flowtree.conf -p &')

os.system('/root/apache-activemq-5.15.0/bin/activemq start')

os.system('sudo mkdir /airflow/')
os.system('sudo mkdir /airflow/plugins/')
os.system('cp /root/executor/flowtree_plugin.py /airflow/plugins/flowtree_plugin.py')
os.system('cp /root/executor/test_plugin.py /airflow/plugins/test_plugin.py')
os.system('rm /airflow/airflow.cfg')
os.system('sudo cp /root/executor/airflow.cfg /airflow');

os.system('airflow initdb')

os.system('ls /airflow/plugins/')
os.system('airflow webserver')