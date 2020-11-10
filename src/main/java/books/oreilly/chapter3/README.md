## RX Operators

### Filter

### Map 

### Flat Map
flatMap() solves these problems by flattening the result so that you get a simple
stream of LicensePlate s. Additionally, in “Multithreading in RxJava” on page 140 we
will learn how to parallelize work using flatMap() . As a rule of thumb, you use flat
Map() for the following situations:
- The result of transformation in map() must be an Observable . For example, per‐
forming long-running, asynchronous operation on each element of the stream
without blocking.
- You need a one-to-many transformation, a single event is expanded into multiple
sub-events. For example, a stream of customers is translated into streams of their
orders, for which each customer can have an arbitrary number of orders.

Notes
- FlatMap does not preserve sequence 

```
Observable
.just(DayOfWeek.SUNDAY, DayOfWeek.MONDAY)
.flatMap(this::loadRecordsFor);
```
The loadRecordsFor() method returns different streams depending on the day of
the week:
```
Observable<String> loadRecordsFor(DayOfWeek dow) {
switch(dow) {
case SUNDAY:
return Observable
.interval(90, MILLISECONDS)
.take(5)
.map(i -> "Sun-" + i);
case MONDAY:
return Observable
.interval(65, MILLISECONDS)
.take(5)
.map(i -> "Mon-" + i);
//...
}
}
```
two streams that work independently but their results must
somehow merge into a single Observable . When flatMap() encounters Sunday in the
upstream, it immediately invokes loadRecordsFor(Sunday) and redirects all events
emitted by the result of that function ( Observable<String> ) downstream. However,
almost exactly at the same time, Monday appears and flatMap() calls loadRecords
For(Monday) . Events from the latter substream are also passed downstream, inter‐
leaving with events from first substream. If flatMap() was suppose to avoid
overlapping it would either need to buffer all subsequent sub- Observable s until the
first one finishes or subscribe to a second sub- Observable only when the first one
completed. Such behavior is actually implemented in concatMap()
But flatMap() instead subscribes to all sub‐
streams immediately and merges them together, pushing events downstream when‐
ever any of the inner streams emit anything. All subsequences returned from
flatMap() are merged and treated equally; that is, RxJava subscribes to all of them
immediately and pushes events downstream evenly:
Mon-0, Sun-0, Mon-1, Sun-1, Mon-2, Mon-3, Sun-2, Mon-4, Sun-3, Sun-4
If you carefully track all delays, you will notice that this order is in fact correct. For
example, even though Sunday was the first event in the upstream Observable , Mon-0
event appeared first because the substream produced by Monday begins emitting
faster. This is also the reason why Mon-4 appears before Sun-3 and Sun-4 .



### Preserving Order Using concatMap()
What if you absolutely need to keep the order of downstream events so that they align
perfectly with upstream events? In other words, downstream events resulted from
upstream event N must occur before events from N + 1 . It turns out there is a handy
concatMap() operator that has the exact same syntax as flatMap() but works quite
differently:
```
Observable
    .just(DayOfWeek.SUNDAY, DayOfWeek.MONDAY)
    .concatMap(this::loadRecordsFor);
```
This time the output is exactly what we anticipated:

Sun-0, Sun-1, Sun-2, Sun-3, Sun-4, Mon-0, Mon-1, Mon-2, Mon-3, Mon-4

So what happened under the hood? When the first event (Sunday) appears from
upstream, concatMap() subscribes to an Observable returned from loadRecords
For() and passes all events emitted from it downstream. When this inner stream
completes, concatMap() waits for the next upstream event (Monday) and continues.
concatMap() does not introduce any concurrency whatsoever but it preserves the
order of upstream events, avoiding overlapping.


### Common Issue
Suppose that you have a large list of users wrapped in an Observable . Each User has a
loadProfile() method that returns an Observable<Profile> instance fetched using
an HTTP request. Our aim is to load the profiles of all users as fast as possible. flat
Map() was designed exactly for that: to allow spawning concurrent computation for
each upstream value:
```
class User {
    Observable<Profile> loadProfile() {
    //Make HTTP request...
    }
}
class Profile {/* ... */}
//...
List<User> veryLargeList = //...
Observable<Profile> profiles = Observable
    .from(veryLargeList)
    .flatMap(User::loadProfile);
```

At first sight it looks great. Observable<User> is constructed from a fixed List using
the from() operator; thus, when subscribed it emits all users pretty much instantane‐
ously. For every new User flatMap() calls, loadProfile() returns Observable<Pro
file> .
Then,flatMap() transparently subscribes to every new Observable<Profile> , 
redirecting all Profile events downstream. Subscription to
inner Observable<Profile> most likely makes a new HTTP connection. Therefore,
if we have, say 10,000 User s, we suddenly triggered 10,000 concurrent HTTP connec‐
tions. If all of them hit the same server, we can expect any of the following:
- Rejected connections
- Long wait time and timeouts
- Crashing the server
- Hitting rate-limit or blacklisting
- Overall latency increase
- Issues on the client, including too many open sockets, threads, excessive memory
usage

Increasing concurrency pays off only up to certain point. If you try to run too many
operations concurrently, you will most likely end up with a lot of context switches,
high memory and CPU utilization, and overall performance degradation. One solu‐
tion could be to slow down Observable<User> somehow so that it does not emit all
User s at once. However, tuning that delay to achieve optimal concurrency level is
troublesome. Instead flatMap() has a very simple overloaded version that limits the
total number of concurrent subscriptions to inner streams:
flatMap(User::loadProfile, 10);
The maxConcurrent parameter limits the number of ongoing inner Observable s. In
practice when flatMap() receives the first 10 User s it invokes loadProfile() for
each of them. However, when the 11th User appears from upstream, 3 flatMap() will
not even call loadProfile() . Instead, it will wait for any ongoing inner streams to
complete. Therefore, the maxConcurrent parameter limits the number of background
tasks that are forked from flatMap() .


### Merge 


### Zip() and zipWith()


We will need to produce a Cartesian product
of all values from two streams. For example we might have two Observable s, one
with chessboard’s rows (ranks, 1 to 8) and one with columns (files, a to h). We would
like to find all possible 64 squares on a chessboard:
```
Observable<Integer> oneToEight = Observable.range(1, 8);

Observable<String> ranks = oneToEight
    .map(Object::toString);

Observable<String> files = oneToEight
    .map(x -> 'a' + x - 1)
    .map(ascii -> (char)ascii.intValue())
    .map(ch -> Character.toString(ch));

Observable<String> squares = files
    .flatMap(file -> ranks.map(rank -> file + rank));
```

The squares Observable will emit exactly 64 events: for 1 it generates a1 , a2 ,... a8 ,
followed by b1 , b2 , and so on until it finally reaches h7 and h8 . This is another inter‐
esting example of flatMap() —for each column (file), generate all possible squares in
that column.

#### A better problem
Suppose that you would like to plan a one-day vacation in some city when the
weather is sunny and airfare and hotels are cheap. To do so, we will combine several
streams together and come up with all possible results:
```
import java.time.LocalDate;
Observable<LocalDate> nextTenDays =
    Observable
        .range(1, 10)
        .map(i -> LocalDate.now().plusDays(i));
Observable<Vacation> possibleVacations = Observable
    .just(City.Warsaw, City.London, City.Paris)
    .flatMap(city -> nextTenDays.map(date -> new Vacation(city, date))
    .flatMap(vacation ->
        Observable.zip(
        vacation.weather().filter(Weather::isSunny),
        vacation.cheapFlightFrom(City.NewYork),
        vacation.cheapHotel(),
        (w, f, h) -> vacation
    ));
```
Vacation class:
```
class Vacation {
    private final City where;
    private final LocalDate when;
    Vacation(City where, LocalDate when) {
        this.where = where;
        this.when = when;
    }
    public Observable<Weather> weather() {
        //...
    }
    public Observable<Flight> cheapFlightFrom(City from) {
        //...
    }
    public Observable<Hotel> cheapHotel() {
        //...
    }
}
```

Quite a lot is happening in the preceding code. First, we generate all dates from
tomorrow to 10 days ahead using a combination of range() and map() . Then, we
flatMap() these days with three cities—we do not want to use zip() here, because
we need all possible combinations of date versus city pairs. For each such pair, we cre‐
ate an instance of Vacation class encapsulating it. Now the real logic: we zip together
three Observable s: Observable<Weather> , Observable<Flight> , and Observa
ble<Hotel> . The last two are supposed to return a zero or one result depending on
whether cheap flight or hotel was found for that city/date. Even though Observa
ble<Weather> always returns something, however, we use filter(Weather::sunny)
to discard nonsunny weather. So we end up with zip() operation of three streams,
each emitting zero to one items. zip() completes early if any of the upstream Observ
able s complete, discarding other streams early: thanks to this property, if any of
weather, flight, or hotel is absent, the result of zip() completes with no items being
emitted, as well. This leaves us with a stream of all possible vacation plans matching
requirements.
Do not be surprised to see a zip function that does not take arguments into account:
(w, f, h) -> vacation . An outer stream of Vacation lists all possible vacation
plans for every possible day. However, for each vacation, we want to make sure
weather, cheap flight, and hotel are present. If all these conditions are met, we return
vacation instance; otherwise, zip will not invoke our lambda expression at all.

### When Streams Are Not Synchronized with One Another: combineLatest(), withLatestFrom(), and amb()
let’s first zip() two streams that are
producing items at the exact same pace:
```
Observable<Long> red
    = Observable.interval(10, TimeUnit.MILLISECONDS);
Observable<Long> green = Observable.interval(10, TimeUnit.MILLISECONDS);
Observable.zip(
        red.timestamp(),
        green.timestamp(),
        (r, g) -> r.getTimestampMillis() - g.getTimestampMillis()
    ).forEach(System.out::println);
```

red and green Observable s are producing items with the same frequency. For each
item, we attach timestamp() so that we know exactly when it was emitted.


![combine latest]()

### Advanced Operators: collect(), reduce(), scan(), distinct(), and groupBy()

### Dropping Duplicates Using distinct() and distinctUntilChanged()

### Asserting Observable Has Exactly One Item Using single()

### Slicing and Dicing Using skip(), takeWhile(), and Others

- take(n) and skip(n)
The take(n) operator will truncate the source Observable prematurely after
emitting only the first n events from upstream, unsubscribing afterward (or com‐
plete earlier if upstream did not have n items). skip(n) is the exact opposite; it
discards the first n elements and begins emitting events from the upstream
Observable beginning with event n+1 . Both operators are quite liberal: negative
numbers are treated as zero, exceeding the Observable size is not treated as a
bug:
```
Observable.range(1, 5).take(3); // [1, 2, 3]
Observable.range(1, 5).skip(3); // [4, 5]
Observable.range(1, 5).skip(5); // []
```

- takeLast(n) and skipLast(n)
Another self-descriptive pair of operators. takeLast(n) emits only the last n val‐
ues from the stream before it completes. Internally, this operator must keep a
buffer of the last n values and when it receives completion notification, it imme‐
diately emits the entire buffer. It makes no sense to call takeLast() on an infinite
stream because it will never emit anything—the stream never ends, so there are
no last events. skipLast(n) , on the other hand, emits all values from upstream
Observable except the last n . Internally, skipLast() can emit the first value from
upstream only when it received n+1 elements, second when it received n+2 , and
so on.
```
Observable.range(1, 5).takeLast(2);// [4, 5]
Observable.range(1, 5).skipLast(2);// [1, 2, 3]

```

- first() and last()
The parameterless first() and last() operators can be implement via
take(1).single() and takeLast(1).single() accordingly, which should pretty
much describe their behavior. The extra single() operator ensures that the
downstream Observable emits precisely one value or exception. Additionally,
both first() and last() have overloaded versions that take predicates. Rather
than returning the very first/last value they emit first/last value, matching a given
condition.

- takeFirst(predicate)
The takeFirst(predicate) operator can be expressed by filter(predi
cate).take(1) . The only difference between this one and first(predicate) is
that it will not break with NoSuchElementException in case of missing matching
values.

- takeUntil(predicate) and takeWhile(predicate)
takeUntil(predicate) and takeWhile(predicate) are closely related to each
other. takeUntil() emits values from the source Observable but completes and
unsubscribes after emitting the very first value matching predicate . take
While() , conversely, emits values as long as they match a given predicate. So the
only difference is that takeUntil() will emit the first nonmatching value,
whereas takeWhile() will not. These operators are quite important because they
provide a means of conditionally unsubscribing from an Observable based on
the events being emitted. Otherwise, the operator would need to somehow inter‐
act with the Subscription instance (see “Controlling Listeners by Using Sub‐
scription and Subscriber<T>” on page 32), which is not available when the
operator is invoked.
```
Observable.range(1, 5).takeUntil(x -> x == 3); // [1, 2, 3]
Observable.range(1, 5).takeWhile(x -> x != 3); // [1, 2]
```

### Exercise
Now it is time for a pop quiz to make sure that you understand how Observable s
work. Look at the following and try to predict what values will be emitted when the
following Observable is subscribed to:
```
Observable
    .just(8, 9, 10)
    .filter(i -> i % 3 > 0)
    .map(i -> "#" + i * 10)
    .filter(s -> s.length() < 4);
```
Observable s are lazy, meaning that they do not begin producing events until some‐
one subscribes. You can create infinite streams that take hours to compute the first
value, but until you actually express your desire to be notified about these events,
Observable is just a passive and idle data structure for some type T . This even applies
to hot Observable s—even though the source of events keeps producing them, not a
single operator like map() or filter() is evaluated until someone actually shows an
interest. Otherwise, running all of these computational steps and throwing away the
result would make no sense. Every time you use any operator, including those that we
did not explain yet, you basically create a wrapper around original Observable . This
wrapper can intercept events flying through it but typically does not subscribe on its
own
First we will walk through the execution path that RxJava takes. Every line in the pre‐
vious code example creates new Observable , in a way wrapping the original one. For
example, the first filter() does not remove 9 from Observable.just(8, 9, 10)
Instead, it creates a new Observable that, when subscribed to, will eventually emit
values 8 and 10 . The same principle applies to most of the operators: they do not
modify the contents or behavior of an existing Observable , they create new ones.
However, saying that filter() or map() creates a new Observable is a bit of a short‐
hand. Most of the operators are lazy until someone actually subscribes. So what hap‐
pens when Rx sees subscribe() at the very end of the chain? Understanding the
internals will help you to realize how streams are processed under the hood. We will
be looking at the code bottom-up.
- First, subscribe() informs the upstream Observable that it wants to receive val‐
ues.
- The upstream Observable ( filter(s -> s.length() < 4) ) does not have any
items by itself, it is just a decorator around another Obervable . So it subscribes to
upstream, as well.
- map(i -> "#" + i * 10) , just like filter() , is not able to deliver any items on
its own. It barely transforms whatever it receives—thus, it must subscribe to
upstream just like the others.
- The story continues until we reach just(8, 9, 10) . This Observable is the true
source of events. As soon as the filter(i -> i % 3 > 0) subscribes to it (as a
consequence of our explicit subscribe() down below), it begins pumping the
events downstream.
- Now we can observe how events are passed through all of the stages of the pipe‐
line. filter() internally receives 8 and passes it downstream ( i % 3 > 0 predicate 
holds). Later on, map() transforms 8 into string "#80" and wakes up the
filter() operator below it.
- The predicate s.length() < 4 holds, and we can finally pass the transformed
value into System.out .

Answer is #20


## Writing Customer Operators
We barely scratched the surface of available operators in RxJava, and you will learn
many more throughout the course of this book. Moreover, the true power of opera‐
tors comes from their composition. Following the UNIX philosophy of "small, sharp
tools,” 9 each operator is doing one, small transformation at a time. This section will
first guide you through the compose() operator, which allows fluent composition of
smaller operators, and later introduces the lift() operator, which helps you to write
entirely new custom operators.

[Read More]()



### Hacks
- Use doOnNext() to log the stream on the way
