# Highlander

Highlander is taking on the "lightings" of data at high speeds and stores this data with the speed that a data store feels comfortable with. 

## Main Ingridients

```
[Non Blocking I/O] => [Queue] => [Data Store]
```

where "Non blocking I/O", "Queue" and "Data Store" pieces are pluggable.

![Java NIO => ZeroMQ => Redis](https://github.com/tolitius/highlander/blob/master/doc/highlander.baseline.gif?raw=true)

#### Defaults
By default Highlander runs on "[Netty](http://netty.io/)" => "[ZeroMQ](http://www.zeromq.org/)" => "[Redis](http://redis.io/)"

There are some built in pieces though that can be used instead of default config:

* "Netty" can be swapped with plain "Java NIO" (in case of a few connections and higher throughput demand)
* "ZeroMQ" can be swapped with a [Single Write Principle Queue](http://mechanical-sympathy.blogspot.com/2011/09/single-writer-principle.html)
* "Redis" can be swapped with "any other" data store

###### _TODO: since a data store (store [thing]) and a queue (produce/consume) are just functions, give an example on how to plug "your own"_

## Current Throughput
 
Benchmark numbers are all relative to many things (hardware, network, clients, other things running, etc..), but to set a rough baseline.. here we go.

An average throughput of a _single_ connection to Highlander with defaults: e.g. `Netty 4.0.24.Final`, `ZeroMQ 4.0.5` (used via JZMQ) and `Redis 2.8.17`, sending 100 byte messages to `127.0.0.1` is **435,000 messages per second**.

This does not mean `435K` messages per second are read from the network and persisted in a data store. The idea is to have queue(s) absorb the load, while landing these messages "comfortably" to the data store. Hence the concept of a `queue depth` (an absorb bucket) shown in examples below.

With `--server nio` (a simple Java NIO server), a _single_ connection speed is **`395K`** messages per second. `Netty` is a default, preferred choice due to its flexibility and ability to scale not only in throughput, but also in a number of connections.

### Throughput with Multiple Clients

By default a built in `bench.streamer` will use 5 clients to send data to highlander. Since Netty likes concurrency, it takes it without a hiccup at an average speed of **1.2 million** 100 byte messages **a second**:

```bash
Nov 24, 2014 12:01:23 PM clojure.tools.logging$eval306$fn__310 invoke
INFO:
total throughput (ALL partitions): 1254800.0 msg/s
```

_(MacBook Pro 2.3 GHz i7 16GB)_

## Usage

####ZeroMQ & Java Bindings

Since "ZeroMQ" is a default queuing mechanism, and Highlander is JVM (Clojure), ZeroMQ [libraries](http://www.zeromq.org/intro:get-the-software) and [Java bindings](http://www.zeromq.org/bindings:java) need to be installed, in order for Highlander to run.

After ZeroMQ and JZMQ are installed, depending on a version of ZeroMQ (let's say it's `${zeromq.version}`), a zeromq jar can be installed to local maven repo. The `zmq.jar` should live in `/usr/local/share/java/`, after JZMQ is installed. Just copy it somewhere, rename it to have a `${zeromq.version}` (e.g. `zmq-${zeromq.version}.jar`), and: 

```bash
mvn install:install-file -Dfile=./zmq-${zeromq.version}.jar -DgroupId=zmq -DartifactId=zmq -Dversion=${zeromq.version} -Dpackaging=jar
```
### Running Highlander

Running it from sources is really simple (that's how we roll in the Clojure Universe):

```bash
$ lein run
```

Once it is up and running, you should see:
```
INFO: highlander is ready for the lightning {:host 0.0.0.0 :port 4242 }
```

and of course the usage:
```bash
Nov 24, 2014 12:11:59 PM clojure.tools.logging$eval306$fn__310 invoke
INFO:  Switches             Default             Desc
 --------             -------             ----
 -h, --host           0.0.0.0             start on this hostname
 -p, --port           4242                listen on this port
 -zq, --zqueue        inproc://zhulk.ipc  use this zmq queue
 -q, --queue          zmq                 queue type [e.g. zmq, swpq]
 -qc, --qcapacity     33554432            queue capacity. used for JVM queues
 -s, --server         netty               server type [e.g. netty, nio]
 -fl, --fixed-length  100                 fixed length messages
 -mi, --monterval     5                   queue monitor interval
```

### Swapping Qs

As you can see from the usage in order to use a Single Write Principle queue instead of ZeroMQ, you can do:

```
$ lein run -q swpq
```

which will reflect that on the startup:

```
...
INFO: [swpq]: single write principle queue is ready to roll
...
```

### Swapping NIO

Using Java NIO instead of Netty can be done via a command line param 's' (or '--server'):

```
$ lein run -s nio
```

### Peeking Inside the Q

While a "Data Store" allows you to easily peek inside, queues are different. 
For example ZeroMQ does not allow you to do that, hence Highlander has a Q monitor built in
that gives basic throughput visibility as the data streams in:

```bash
---------------------------------------------
queue  [inproc://zhulk.ipc:29]
---------------------------------------------
       message rate: 445253.2 msg/s
      current depth: 8359612
 pass through total: 8619778
```

### Exporting LD_LIBRARY_PATH

Before running Highlander, `LD_LIBRARY_PATH` needs to be exported to let it know where to find ZeroMQ C++/Java libs:
```bash
export LD_LIBRARY_PATH=/usr/local/lib
```


### Q Pusher

There is a bench playground that has a basic Java NIO streamer, as well as a Q Pusher, which allows to take network (Netty/NIO) and Data Store out of the picture to just focus on Q numbers. It can be run as:

```bash
$ lein run -m bench.qpusher
```

```bash
INFO: Usage:

 Switches          Default         Desc
 --------          -------         ----
 -zq, --zqueue     inproc://zhulk  use this zmq queue
 -q, --queue       zmq             queue type (e.g. zmq, swpq)
 -n, --number      100000000       number of things
 -qc, --qcapacity  33554432        queue capacity. used for JVM queues
 -mi, --monterval  5               queue monitor interval
```

For example here is a default ZeroMQ rate with a single queue pushing 2.8 million messages a second:

```bash
---------------------------------------------
queue  [inproc://zhulk:1]
---------------------------------------------
       message rate: 2850446.8 msg/s
      current depth: 27747444
 pass through total: 27755805
```

While ZeroMQ is great and it is default, the Q Pusher can be told to work with any other queue implementaion. 
For example here is how to tell a Q Pusher to work with a built in Single Write Principle Queue, 
which is a pure JVM queue implementation:

```
$ lein run -m bench.qpusher -q swpq
```

```
       message rate: 167536.0 msg/s
      current depth: 40987563
 pass through total: 41007837
```

### Bench Streamer

Highlander also has a NIO load simulation streamer that can be run on the same or different host and stream messages to a Highlander instance.

Since this is a simulation streamer, it needs to be as close to the true load as possible, hence it streams "pure truth" in a form of a 100 byte question:

```java
public static final String ULTIMATE_TRUTH = 
  "[Did you know that the Answer to the Ultimate Question of Life, the Universe, and Everything is 42?]";
```

that is where a "stream of truth" comes from.

#### Usage

```bash
 Switches                Default                                                                                               Desc
 --------                -------                                                                                               ----
 -h, --host              0.0.0.0                                                                                               start on this hostname
 -p, --port              4242                                                                                                  listen on this port
 -c, --clients           5                                                                                                     number of clients
 -n, --number-of-things  200000000                                                                                             number of things to stream
 -t, --thing             [Did you know that the Answer to the Ultimate Question of Life, the Universe, and Everything is 42?]  a thing/message to send
```
e.g.
```bash
$ lein run -m bench.streamer -h 10.32.2.1 -c 10
```

By default `lein run -m bench.streamer` will stream with 5 (clients) * 200 million of "100 byte" truths to a Highlander instance. 

Just as with Highlander, before running bench streamer, `LD_LIBRARY_PATH` needs to be exported to let it know where to find ZeroMQ C++/Java libs:
```bash
export LD_LIBRARY_PATH=/usr/local/lib
```

## License

Copyright © 2014 tolitius

Distributed under the Eclipse Public License, the same as Clojure.
