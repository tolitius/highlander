# Highlander

Highlander is taking on the "lightings" of data at high speeds and stores this data with the speed that a data store feels comfortable with. Its main ingridients are: 

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

## Usage

Since "ZeroMQ" is a default queuing mechanism, and Highlander is JVM (Clojure), ZeroMQ [libraries](http://www.zeromq.org/intro:get-the-software) and [Java bindings](http://www.zeromq.org/bindings:java) need to be installed, in order for Highlander to run.

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

While a "Data Store" allows you to easily peek inside, queues are different. For example ZeroMQ does not allow you to do that, hence Highlander has a Q monitor built in that gives basic throughput visibility as the data streams in:

```bash
       message rate: 72047.6 msg/s
      current depth: 143428
 pass through total: 7939711
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

For example here is a default ZeroMQ rate pushing 2 million messages a second:

```bash
INFO: pushed  14000000  things
Jul 23, 2013 9:20:13 AM clojure.tools.logging$eval9$fn__13 invoke
INFO:
       message rate: 2019744.2 msg/s
      current depth: 29474713
 pass through total: 29488869
Jul 23, 2013 9:20:13 AM clojure.tools.logging$eval9$fn__13 invoke
INFO: pushed  15000000  things
```

While ZeroMQ is great and it is default, the Q Pusher can be told to work with any other queue implementaion. 
For example here is how to tell a Q Pusher to work with a built in Single Write Principle Queue, 
which is a pure JVM queue implementation:

```
$ lein run -m bench.qpusher -q swpq
```

```
INFO: pushed  13000000  things
Jul 23, 2013 9:21:53 AM clojure.tools.logging$eval9$fn__13 invoke
INFO:
       message rate: 1140890.0 msg/s
      current depth: 27507363
 pass through total: 27527867
Jul 23, 2013 9:21:54 AM clojure.tools.logging$eval9$fn__13 invoke
INFO: pushed  14000000  things
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

## License

Copyright © 2013 tolitius

Distributed under the Eclipse Public License, the same as Clojure.
