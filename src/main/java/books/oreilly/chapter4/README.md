### Intro

We take you through some common application patterns and ways by which you can
enhance them with RxJava in noninvasive way, with the focus being on database
querying, caching, error handling, and periodic tasks. The more RxJava you add in
various places of your stack the more consistent your architecture will become.


### From Collections to Observables
Unless your platform was built recently in JVM frameworks like Play, Akka actors, or
maybe Vert.x, you are probably on a stack with a servlet container on one hand, and
JDBC or web services on the other. Between them, there is a varying number of layers
implementing business logic, which we will not refactor all at once; instead, let’s begin
with a simple example. The following class represents a trivial repository abstracting
us from a database:
```
class PersonDao {
    List<Person> listPeople() {
        return query("SELECT * FROM PEOPLE");
    }
    private List<Person> query(String sql) {
        //...
    }
}
```

Implementation details aside, how is this related to Rx? So far we have been talking
about asynchronous events pushed from upstream systems or, at best, when someone
subscribes. How is this mundane Dao relevant here? Observable is not only a pipe
pushing events downstream. You can treat Observable<T> as a data structure, dual to
Iterable<T> . They both hold items of type T , but providing a radically different
interface. So, it shouldn’t come as a surprise that you can simply replace one with the
other:
```
Observable<Person> listPeople() {
    final List<Person> people = query("SELECT * FROM PEOPLE");
    return Observable.from(people);
}
```

At this point, we made a breaking change to the existing API. Depending on how big
your system is, such incompatibility might be a major concern. Thus, it is important
to bring RxJava into your API as soon as possible. Obviously, we are working with an
existing application so that can’t be the case.


#### Let's not make that big change lets refactor our existing code base from non-reactive to reactive
Observable is not an Iterable in any sense, so our
code no longer compiles. We want to take baby steps rather than massive refactoring,
so let’s keep the scope of changes as minimal as possible. The client code could look
like this:
```
List<Person> people = pesonDao.listPeople();
String json = marshal(people);
```

### Embracing Laziness
So how do we make our Observable lazy? The simples technique is to wrap an eager
Observable with defer() :
```
public Observable<Person> listPeople() {
    return Observable.defer(() ->
    Observable.from(query("SELECT * FROM PEOPLE")));
}
```

Observable.defer() takes a lambda expression (a factory) that can produce Observa
ble . The underlying Observable is eager, so we want to postpone its creation.
defer() will wait until the last possible moment to actually create Observable ; that is,
until someone actually subscribes to it. This has some interesting implications.
Because Observable is lazy, calling listPeople() has no side effects and almost no
performance footprint. No database is queried yet. You can treat Observable<Per
son> as a promise but without any background processing happening yet. Notice that
there is no asynchronous behavior at the moment, just lazy evaluation. This is similar
to how values in the Haskell programming language are evaluated lazily only when
absolutely needed.

![concat]() 

### Lazy paging and concatenation
There are more ways to implement lazy paging with RxJava. If you think about it, the
simplest way of loading paged data is to load everything and then take whatever we
need. It sounds silly, but thanks to laziness it is feasible. First we generate all possible
page numbers and then we request loading each and every page individually:
```
Observable<List<Person>> allPages = Observable
    .range(0, Integer.MAX_VALUE)
    .map(this::listPeople)
    .takeWhile(list -> !list.isEmpty());
```

### Imperative Concurrency
- Observables are lazy by default
- traditional blocking programs and the one with Observable work exactly the
    same way
- Two types of observable - Hot and Cold


It is tempting to think that subscribeOn() is the right tool for con‐
currency in RxJava. This operator works but you should not see the
usage of subscribeOn() (and yet to be described observeOn() )
often. In real life, Observable s come from asynchronous sources,
so custom scheduling is not needed at all. We use subscribeOn()
throughout this chapter to explicitly show how to upgrade existing
applications to use reactive principles selectively. But in practice,
Scheduler s and subscribeOn() are weapons of last resort, not
something seen commonly.


### flatMap() as Asynchronous Chaining Operator


### Replacing Callbacks with Streams
Traditional APIs are blocking most of the time, meaning they force you to wait syn‐
chronously for the results. This approach works relatively well, at least before you
heard about RxJava. But a blocking API is particularly problematic when data needs
to be pushed from the API producer to consumers—this is anarea where RxJava
really shines. There are numerous examples of such cases and various approaches are
taken by API designers. Typically, we need to provide some sort of a callback that the
API invokes, often called event listeners. One of the most common scenarios like that
is Java Message Service (JMS). Consuming JMS typically involves implementing a
class that the application server or container notifies on every incoming messages. We
can replace with relative ease such listeners with a composable Observable , which is
much more robust and versatile.

- The easiest way to convert from a push,
callback-based API to Observable is to use Subject s.

As a side note, Subject s are easier to get started but are known to be problematic
after a while. In this particular case, we can easily replace Subject with the more
idiomatic RxJava Observable that uses create() directly:

### What Is a Scheduler?
RxJava is concurrency-agnostic, and as a matter of fact it does not introduce concur‐
rency on its own. However, some abstractions to deal with threads are exposed to the
end user. Also, certain operators cannot work properly without concurrency; see
“Other Uses for Schedulers” on page 163 for some of them. Luckily, the Scheduler
class, the only one you must pay attention to, is fairly simple. In principle it works
similarly to ScheduledExecutorService from java.util.concurrent —it executes
arbitrary blocks of code, possibly in the future. However, to meet Rx contract, it offers
some more fine-grained abstractions, which you can see more of in the advanced sec‐
tion “Scheduler implementation details overview” on page 146.
Schedulers are used together with subscribeOn() and observeOn() operators as well
as when creating certain types of Observable s. A scheduler only creates instances of
Worker s that are responsible for scheduling and running code. When RxJava needs to
schedule some code it first asks Scheduler to provide a Worker and uses the latter to
schedule subsequent tasks. You will find examples of this API later on, but first famil‐
iarize yourself with available built-in schedulers:

- Schedulers.newThread()
This scheduler simply starts a new thread every time it is requested via subscri
beOn() or observeOn() . newThread() is hardly ever a good choice, not only
because of the latency involved when starting a thread, but also because this
thread is not reused. Stack space must be allocated up front (typically around one
megabyte, as controlled by the -Xss parameter of the JVM) and the operating
system must start new native thread. When the Worker is done, the thread simply
terminates. This scheduler can be useful only when tasks are coarse-grained: it
takes a lot of time to complete but there are very few of them, so that threads are
unlikely to be reused at all. See also: “Thread per Connection” on page 329. In
practice, following Schedulers.io() is almost always a better choice.

- Schedulers.io()
This scheduler is similar to newThread() , but already started threads are recycled
and can possibly handle future requests. This implementation works similarly to
ThreadPoolExecutor from java.util.concurrent with an unbounded pool of
threads. Every time a new Worker is requested, either a new thread is started (and
later kept idle for some time) or the idle one is reused.
The name io() is not a coincidence. Consider using this scheduler for I/O bound
tasks which require very little CPU resources. However they tend to take quite
some time, waiting for network or disk. Thus, it is a good idea to have a relatively
big pool of threads. Still, be careful with unbounded resources of any kind—in
case of slow or unresponsive external dependencies like web services, io()
scheduler might start an enormous number of threads, leading to your very own
application becoming unresponsive, as well. See “Managing Failures with Hys‐
trix” on page 291 for more details how to tackle this problem.


- Schedulers.computation()
You should use a computation scheduler when tasks are entirely CPU-bound;
that is, they require computational power and have no blocking code (reading
from disk, network, sleeping, waiting for lock, etc.) Because each task executed
on this scheduler is supposed to fully utilize one CPU core, executing more such
tasks in parallel than there are available cores would not bring much value.
Therefore, computation() scheduler by default limits the number of threads run‐
ning in parallel to the value of availableProcessors() , as found in the Run
time.getRuntime() utility class.
If for some reason you need a different number of threads than the default, you
can always use the rx.scheduler.max-computation-threads system property.
By taking less threads you ensure that there is always one or more CPU cores
idle, and even under heavy load, computation() thread pool does not saturate
your server. It is not possible to have more computation threads than cores.
computation() scheduler uses unbounded queue in front of every thread, so if
the task is scheduled but all cores are occupied, they are queued. In case of load
peak, this scheduler will keep the number of threads limited. However, the queue
just before each thread will keep growing.




#### Note
Lazy loading is heart of functional programming



