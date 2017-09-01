# Almost Realism FlowTree #

FlowTree is an executor for Apache Airflow. It is similar to Celery, except it has a number of differences that make
it a better choice for some users.

## Why use FlowTree? ##

There are a number of reasons you may prefer FlowTree to other executors. They are listed in order of importance below.

### Distributed File System ###
FlowTree provides its own file system which supports lazy loading. What this means is that you can have resources
scattered all over your cluster of workers and your jobs can access them as if they were all in the same place.
No data is actually transfered between machines in the cluster until a resource is accessed and the minimal amount
of data that is required to respond to a file load operation is retrieved. This eliminates the need to have some
kind of repository checked out on each machine in the cluster.
### Cloud Native / Docker Native ###
FlowTree is designed to be run anywhere. To achieve this portability it (1) is written in Java, a platform-independent
language, (2) has a very minimal set of dependencies, none of which are native/OS-specific code, and (3) is available
as a docker image that requires no configuration. For users that do need to customize the environment it is easy to use
the official docker image as a starting point and extend it.

Run FlowTree: https://hub.docker.com/r/almostrealism/parallelize/
### Peer-to-Peer Deployment Methodology ###
FlowTree works similarly to BitTorrent or other Peer-to-Peer networks. When an installation starts up it will look for
peers to connect to, and peers can also be added manually. FlowTree also supports restructuring behaviors which will
automatically perform operations on the layout of the Peer-to-Peer network to improve performance. For example, one
installation may look at the peers of another installation and discover that one of those peers has a lower ping time
than the peer that is currently connected. One of the default available restructuring behaviors will recognize this
scenario and connect with the other peer to improve the response time of requests on the distributed file system.
### Best Machine for the Job ###
Through FlowTree's gravity mechanism, jobs are drawn toward (ie, more likely to execute on) machines which are better
suited to the job. This is accomplished through flags that include things like "prefers GPU" or "needs > Xm ram", etc.
### Distributed Security ###
FlowTree's security model is designed to be geniunly distributed. A FlowTree installation may connect with another only
if firewall rules allow for it and since FlowTree allows for complex peer-to-peer networks the distributed file system
is designed to respect the structure of the network meaning that data for a particular resource can only be retrieved if
there is a legitimate path from the installation requesting the data to the installation that has the data.
### No Single Point of Failure ###
Most execus for Airflow rely on a single point of failure for the task queue. Because FlowTree is a distributed system
there is no single point of failure.
### Ad Hoc Network Layouts ###
FlowTree installations can be set up in any structure you wish. When one FlowTree installation is reaching maximal capacity
it begins to send jobs to peers to try and reduce the load. As a result, different network structures will have unique impacts
on how the load is rebalanced. A linear arrangement will function like a priority queue, while a highly connected network will
function more like one large VM. Fine tuning the network layout often becomes necessary when you start to create FlowTree
networks that span multiple datacenters.

## Licensing & Authorship ##
My name is Michael Murray and I am the author of this tool (though I would enthusiastically
accept collaborators). For more information on my background you can visit http://michaelDmurray.com

If you want to use the tools provided here, or you are interested in the concept of open source
art pieces and want to contribute please contact ashesfall@almostrealism.com for help.

```
#!java

Copyright 2004-2017 Michael Murray

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Show Your Support ##

If you are interested in helping me continue this work, donations can be submitted via [PayPal](https://paypal.me/discomike) and are **appreciated**. I also create music with some of the tools here, and if you are interested in supporting that you can join my [Patreon](https://www.patreon.com/user?u=3646756) page. I am also looking for a full time software job so please make sure to check out my [LinkedIn](https://www.linkedin.com/in/ashesfall) page.

## Package Overview 🔶 ##
Distributed parallel processing system for computer graphics, physics and more. *This project requires a retired apache project (**Slide**) and will be upgraded to use a different WebDav library in the future. Also, this project uses HSQLDB optionally (by default), but can be configured to use any SQL relational database.*

## Maintainence Schedule ##
| **FlowTree** is on an apprximately 10 week lag. I can fix bugs on that horizon.