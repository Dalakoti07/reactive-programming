### Hot and cold stream

### Async versus Sync

### Lazy versus Eager
The Observable type is lazy, meaning it does nothing until it is subscribed to. This
differs from an eager type such as a Future , which when created represents active
work. Lazyiness allows composing Observables together without data loss due to
race conditions without caching. In a Future , this isn’t a concern, because the single
value can be cached, so if the value is delivered before composition, the value will be
fetched. With an unbounded stream, an unbounded buffer would be required to pro‐
vide this same guarantee. Thus, the Observable is lazy and will not start until subscri‐
bed to so that all composition can be done before data starts flowing.


### Conclusions
Event loop gives following features :

- First, better latency and throughput means both better user experience and lower infrastructure cost. 
- Second, though, the event-loop architecture is more resilient under load. Instead of falling apart when the
load is increased, the machine can be pushed to its limit and handles it gracefully.
This is a very compelling argument for large-scale production systems that need to
handle unexpected spikes of traffic and remain responsive.

I also found the event-loop architecture easier to operate. It does not 1 require tuning
to get optimal performance, whereas the thread-per-request architecture often needs
tweaking of thread pool sizes (and subsequently garbage collection) depending on
workload.

This is not intended to be an exhaustive study of the topic, but I found this experiment and resulting data as compelling evidence for pursuing the “reactive” architec‐
ture in the form of nonblocking IO and event loops. In other words, with hardware,
the Linux kernel, and JVM circa 2015/2016, nonblocking I/O via event loops does
have benefits.


### Reactive Abstraction
Ultimately RxJava types and operators are just an abstraction over imperative call‐
backs. However, this abstraction completely changes the coding style and provides
very powerful tools for doing async and nonblocking programming. It takes effort to
learn and requires a shift of thinking to be comfortable with function composition
and thinking in streams, but when you’ve achieved this it is a very effective tool
alongside our typical object-oriented and imperative programming styles.


