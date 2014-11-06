# Highlander

Highlander is taking on the "lightings" of data at high speeds and stores this data with the speed that a data store feels comfortable with. 

## Main Ingridients

```
[Non Blocking I/O] => [Queue] => [Data Store]
```

where "Non blocking I/O", "Queue" and "Data Store" pieces are pluggable.

_TODO: update gif to reflect the current 400K+ messages per second_
![Java NIO => ZeroMQ => Redis](https://github.com/tolitius/highlander/blob/master/doc/highlander.baseline.gif?raw=true)

#### Defaults
By default Highlander runs on "[Netty](http://netty.io/)" => "[ZeroMQ](http://www.zeromq.org/)" => "[Redis](http://redis.io/)"

There are some built in pieces though that can be used instead of default config:

* "Netty" can be swapped with plain "Java NIO" (in case of a few connections and higher throughput demand)
* "ZeroMQ" can be swapped with a [Single Write Principle Queue](http://mechanical-sympathy.blogspot.com/2011/09/single-writer-principle.html)
* "Redis" can be swapped with "any other" data store

###### _TODO: since a data store (store [thing]) and a queue (produce/consume) are just functions, give an example on how to plug "your own"_

## Current Throughput

With `--server nio` and `ZeroMQ 4.0.5` (used via JZMQ), average throughput on `127.0.0.1` is **`420K`** `107 byte` messages per second. 

This does not mean `420K` messages per second are read from the network and persisted in a data store. The idea is to have queue(s) absorb the load, while landing these messages "comfortably" to the data store. Hence the concept of a `queue depth` (absorb bucket) shown in examples below.

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
Mar 21, 2013 8:10:30 PM clojure.tools.logging$eval3$fn__7 invoke
INFO: Usage:

 Switches          Default             Desc
 --------          -------             ----
 -h, --host        0.0.0.0             start on this hostname
 -p, --port        4242                listen on this port
 -zq, --zqueue     inproc://zhulk.ipc  use this zmq queue
 -q, --queue       zmq                 queue type [e.g. zmq, swpq]
 -qc, --qcapacity  33554432            queue capacity. used for JVM queues
 -s, --server      netty               server type [e.g. netty, nio]
 -mi, --monterval  5                   queue monitor interval
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
that gives basic throughput visibility as the data streams in (numbers via "--server nio"):

```bash
       message rate: 419482.8 msg/s
      current depth: 3550038
 pass through total: 9874863
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

For example here is a default ZeroMQ rate pushing 6.7 million messages a second:

```bash
       message rate: 6692981.6 msg/s
      current depth: 130530939
 pass through total: 130547905
```

While ZeroMQ is great and it is default, the Q Pusher can be told to work with any other queue implementaion. 
For example here is how to tell a Q Pusher to work with a built in Single Write Principle Queue, 
which is a pure JVM queue implementation:

```
$ lein run -m bench.qpusher -q swpq
```

```
       message rate: 1676536.0 msg/s
      current depth: 40987563
 pass through total: 41007837
```

### Bench Streamer

Highlander also has a NIO load simulation streamer that can be run on the same or different host and stream messages to a Highlander instance.

Since this is a simulation streamer, it needs to be as close to the true load as possible, hence it streams "pure truth" in a form of a question:

```java
public static final String ULTIMATE_TRUTH = 
  "Did you know that the Answer to the Ultimate Question of Life, the Universe, and Everything is 42? Did you?";
```

that is where a "stream of truth" comes from.

#### Usage

```bash
Streamer host port [number of things to stream]
```
e.g.
```bash
$ lein run -m bench.Streamer localhost 4242
```

will, by default, stream 100 million of "107 byte" truths to a Highlander instance. 

Just as with Highlander, before running Bench Streamer, `LD_LIBRARY_PATH` needs to be exported to let it know where to find ZeroMQ C++/Java libs:
```bash
export LD_LIBRARY_PATH=/usr/local/lib
```

## License

Copyright © 2014 tolitius

Distributed under the Eclipse Public License, the same as Clojure.
