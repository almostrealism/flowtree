# -*- coding: utf-8 -*-
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from builtins import object
import logging
import subprocess
import time

from airflow.exceptions import AirflowException
from airflow.executors.base_executor import BaseExecutor
from airflow import configuration

PARALLELISM = configuration.get('core', 'PARALLELISM')

DEFAULT_QUEUE = configuration.get('flowtree', 'DEFAULT_QUEUE')

app = FlowTree()


@app.task
def execute_command(command):
    try:
        subprocess.check_call(command, shell=True)
    except subprocess.CalledProcessError as e:
        logging.error(e)
        raise AirflowException('FlowTree command failed')


class FlowTreeExecutor(BaseExecutor):
    def start(self):
        self.tasks = {}
        self.last_state = {}

    def execute_async(self, key, command, queue=DEFAULT_QUEUE):
        self.logger.info( "[flowtree] queuing {key} through flowtree, "
                          "queue={queue}".format(**locals()))
        self.tasks[key] = execute_command.apply_async(
            args=[command], queue=queue)
        self.last_state[key] = flowtree_states.PENDING

    def sync(self):

        self.logger.debug(
            "Inquiring about {} Flow Tree task(s)".format(len(self.tasks)))
        for key, async in list(self.tasks.items()):
            state = async.state
            if self.last_state[key] != state:
                if state == flowtree_states.SUCCESS:
                    self.success(key)
                    del self.tasks[key]
                    del self.last_state[key]
                elif state == flowtree_states.FAILURE:
                    self.fail(key)
                    del self.tasks[key]
                    del self.last_state[key]
                elif state == flowtree_states.REVOKED:
                    self.fail(key)
                    del self.tasks[key]
                    del self.last_state[key]
                else:
                    self.logger.info("Unexpected state: " + async.state)
                self.last_state[key] = async.state

    def end(self, synchronous=False):
        if synchronous:
            while any([
                        async.state not in flowtree_states.READY_STATES
                for async in self.tasks.values()]):
                time.sleep(5)
        self.sync()
