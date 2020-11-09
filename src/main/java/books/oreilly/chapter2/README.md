### Some points
In RxJava, Observable<T> is just a typed data structure that can exist very
briefly or for many days, as long as a server application runs.


Observable.create() is fundamental, we can create all types of observables using this.

```
Observable<Integer> ints =
    Observable.create(subscriber -> {
        log("Create");
        subscriber.onNext(42);
        subscriber.onCompleted();
    }
);

log("Starting");
ints.subscribe(i -> log("Element A: " + i));
ints.subscribe(i -> log("Element B: " + i));
log("Exit");

```

What kind of output do you expect? Remember that every time you subscribe to an
Observable created via the create() factory method, the lambda expression passed
as an argument to create() is executed independently by default within the thread
that initiated the subscription:

```
Ouput

main:Starting
main:Create
main:Element A: 42
main:Create
main:Element B: 42
main:Exit
```

If you would like to avoid calling create() for each subscriber and simply reuse
events that were already computed, there exists a handy cache() operator:

```
Observable<Integer> ints =
    Observable.<Integer>create(subscriber -> {
        //...
        }
    )
    .cache();
```

cache() is the first operator that you learn. Operators wrap existing Observable s,
enhancing them, typically by intercepting subscription. What cache() does is stand
between subscribe() and our custom Observable . When the first subscriber
appears, cache() delegates subscription to the underlying Observable and forwards
all notifications (events, completions, or errors) downstream. However, at the same
time, it keeps a copy of all notifications internally. When a subsequent subscriber
wants to receive pushed notifications, cache() no longer delegates to the underlying
Observable but instead feeds cached values. With caching, the output for two Sub
scriber s is quite different:

```
Output

main:Starting
main:Create
main:Element A: 42
main:Element B: 42
main:Exit
```

Of course, you must keep in mind that cache() plus infinite stream is the recipe for a
disaster, also known as OutOfMemoryError .


#### Note 
In rx-java exceptions are first class citizens

### Timing: timer() and interval()
timer() and interval() . The former simply creates an Observable that emits a long
value of zero after a specified delay and then completes:
```
Observable
    .timer(1, TimeUnit.SECONDS)
    .subscribe((Long zero) -> log(zero));
```

As silly as it sounds, timer() is extremely useful. It is basically an asynchronous
equivalent of Thread.sleep() . Rather than blocking the current thread, we create an
Observable and subscribe() to it. It will become significantly more important after
we learn how to compose simple Observable s into more complex computations. The
fixed value of 0 (in variable zero ) is just a convention without any specific meaning.
However, it makes more sense when interval() is introduced. interval() generates
a sequence of long numbers, beginning with zero, with a fixed delay between each
one of them:
```
Observable
    .interval(1_000_000 / 60, MICROSECONDS)
    .subscribe((Long i) -> log(i));
```

Observable.interval() produces a sequence of consecutive long numbers, begin‐
ning with 0 . However, unlike range() , interval() places a fixed delay before every
event, including the first one. In our example, this delay is about 16666 μs, which
roughly corresponds to 60 Hz, which is the frame rate often used in various animations. 
This is not a coincidence: interval() is sometimes used to control animations
or processes that need to run with certain frequency. interval() is somewhat similar
to scheduleAtFixedRate() from ScheduledExecutorService . You can probably
imagine multiple usage scenarios of interval() , like periodic polling for data,
refreshing user interfaces, or modeling elapsing time in simulation.


### Hot and cold observable

##### Hot
A cold Observable is entirely lazy and never
begins to emit events until someone is actually interested. If there are no observers,
Observable is just a static data structure. This also implies that every subscriber
receives its own copy of the stream because events are produced lazily but also not
likely cached in any way. Cold Observable s typically come from Observable.cre
ate() , which idiomatically should not start any logic but instead postpone it until
someone actually listens. A cold Observable is thus somewhat dependent on Sub
scriber . Examples of cold Observable s, apart from create() , include Observa
ble.just() , from() , and range() . Subscribing to a cold Observable often involves a
side effect happening inside create() . For example, the database is queried or a con‐
nection is opened.

##### Cold
Hot Observable s are different. After you get a hold of such an Observable it might
already be emitting events no matter how many Subscriber s they have. Observable
pushes events downstream, even if no one listens and events are possibly missed.
Whereas typically you have full control over cold Observable s, hot Observable s are
independent from consumers. When a Subscriber appears, a hot Observable
behaves like a wire tap, 2 transparently publishing events flowing through it. The pres‐
ence or absence of Subscriber does not alter the behavior of Observable ; it is
entirely decoupled and independent.

### Single Subscription with publish().refCount()
Let us recap: we have a single handle to the underlying resource; for example, HTTP
connection to stream of Twitter status updates. However, an Observable pushing
these events will be shared among multiple Subscriber s. The naive implementation
of this Observable created earlier had no control over this; therefore, each Subscriber
started its own connection. This is quite wasteful:
```
Observable<Status> observable = Observable.create(subscriber -> {
    System.out.println("Establishing connection");
    TwitterStream twitterStream = new TwitterStreamFactory().getInstance();
    //...
    subscriber.add(Subscriptions.create(() -> {
        System.out.println("Disconnecting");
        twitterStream.shutdown();
        }));
    twitterStream.sample();
});
```
When we try to use this Observable , each Subscriber establishes a new connection,
like so:
```
Subscription sub1 = observable.subscribe();
System.out.println("Subscribed 1");
Subscription sub2 = observable.subscribe();
System.out.println("Subscribed 2");
sub1.unsubscribe();
System.out.println("Unsubscribed 1");
sub2.unsubscribe();
System.out.println("Unsubscribed 2");

Here is the output:

Establishing connection
Subscribed 1
Establishing connection
Subscribed 2
Disconnecting
Unsubscribed 1
Disconnecting
Unsubscribed 2
```

This time, to simplify, we use a parameterless subscribe() method that triggers subscription 
but drops all events and notifications. After spending almost half of the
chapter fighting with this problem and familiarizing ourselves with plenty of RxJava
features, we can finally introduce the most scalable and simplest solution: the pub
lish().refCount() pair:
```
lazy = observable.publish().refCount();
//...
System.out.println("Before subscribers");
Subscription sub1 = lazy.subscribe();
System.out.println("Subscribed 1");
Subscription sub2 = lazy.subscribe();
System.out.println("Subscribed 2");
sub1.unsubscribe();
System.out.println("Unsubscribed 1");
sub2.unsubscribe();
System.out.println("Unsubscribed 2");

The output is much like what we expect:

Before subscribers
Establishing connection
Subscribed 1
Subscribed 2
Unsubscribed 1
Disconnecting
Unsubscribed 2
```

The connection is not established until we actually get the first Subscriber . But,
more important, the second Subscriber does not initiate a new connection, it does
not even touch the original Observable .The publish().refCount() tandem wrap‐
ped the underlying Observable and intercepted all subscriptions. We will explain
later why we need two methods and what using publish() alone means. For the time
being, we will focus on refCount() . What this operator does is basically count how
many active Subscriber s we have at the moment, much like reference counting in
historic garbage-collection algorithms. When this number goes from zero to one, it
subscribes to the upstream Observable . Every number above one is ignored and the
same upstream Subscriber is simply shared between all downstream Subscriber s.
However, when the very last downstream Subscriber unsubscribes, the counter
drops from one to zero and refCount() knows it must unsubscribe right away.
Thankfully, refCount() does precisely what we implemented manually with Lazy
TwitterObservable . You can use the publish().refCount() duet to allow sharing of
a single Subscriber while remaining lazy. This pair of operators is used very fre‐
quently and therefore has an alias named share() . Keep in mind that if unsubscrip‐
tion is shortly followed by subscription, share() still performs reconnection, as if
there were no caching at all.

### Summary
Creating and subscribing to Observable are essential features of RxJava. Especially
beginners tend to forget about subscription and are surprised that no events are emit‐
ted. Many developers focus on amazing operators provided by this library ,but failing to understand how these operators perform subscription
underneath can cause subtle bugs.
Moreover, the asynchronous nature of RxJava is typically taken for granted, which is
not really the case. As a matter of fact, most operators in RxJava do not use any particular 
thread pool. More precisely this means that by default no concurrency is
involved whatsoever and everything happens in client thread. This is another important 
take away of this chapter. Now, when you understand subscription and 
concurrency principles, you are ready to begin using RxJava painlessly and effectively.


