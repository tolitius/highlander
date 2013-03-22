# Highlander

Highlander is taking on the "lightings" of data at high speeds and stores this data with the speed that a data store feels comfortable with. Its main ingridients are: 

```
[Non Blocking I/O] => [Queue] => [Data Store]
```

where a "Non blocking I/O" is [Netty](http://netty.io/) with pluggable "Queue" and "Data Store" pieces.

#### Defaults
By default Highlander runs on "Netty" => "ZeroMQ" => "Redis"
with an option to swap out "ZeroMQ" with a [Single Write Principle Queue](http://mechanical-sympathy.blogspot.com/2011/09/single-writer-principle.html)

##### _TODO: since a data store (store [thing]) and a queue (produce/consume) are just functions, give an example on how to plug "your own"_

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

### Peeking Inside the Q

While a "Data Store" allows you to easily peek inside, queues are different. For example ZeroMQ does not allow you to do that, hence Highlander has a Q monitor built in that gives basic throughput visibility as the data streams in:

```bash
       message rate: 329552.2 msg/s
      current depth: 143428
 pass through total: 7939711
```

### Exporting LD_LIBRARY_PATH

Before running Highlander, `LD_LIBRARY_PATH` needs to be exported to let it know where to find ZeroMQ C++/Java libs:
```bash
export LD_LIBRARY_PATH=/usr/local/lib
```


### Q Pusher

There is a bench playground that has a basic Java NIO streamer, as well as a Q Pusher, which allows to take Netty and Data Store out of the picture to just focus on Q numbers. It can be run as:

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

By default it is ZeroMQ, but it can be switched to another queue (e.g. Single Write Principle):

```
$ lein run -m bench.qpusher -q swpq
```

### Bench Streamer

usage:

```bash
Streamer host port [number of things to stream]
```

e.g.

```bash
$ lein run -m bench.Streamer localhost 4242
```

will, by default, stream 100 million of "107 byte" things to a Highlander instance. 

## License

Copyright © 2013 tolitius

Distributed under the Eclipse Public License, the same as Clojure.
