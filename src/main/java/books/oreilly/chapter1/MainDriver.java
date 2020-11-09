package books.oreilly.chapter1;

import io.reactivex.Observable;

import java.util.HashMap;

public class MainDriver {

    public static String getFromCache(String kay){
        if(kay!=null)
            return "fake value";
        return null;
    }

    public static void main(String[] args) {

        // thi is synchronous observable stream
        //The actual criteria that is generally
        //important is whether the Observable event production is blocking or nonblocking,
        //not whether it is synchronous or asynchronous
        Observable.create(s -> {
            s.onNext("Hello World!");
            s.onComplete();
        }).subscribe(streamString -> System.out.println(streamString));

        //there are two good reasons to use synchronous behavior, which we’ll look at
        //in the following subsections.

        // in memory data
        //If data exists in a local in-memory cache (with constant microsecond/nanosecond
        //lookup times), it does not make sense to pay the scheduling cost to make it asynchro‐
        //nous. The Observable can just fetch the data synchronously and emit it on the sub‐
        //scribing thread, as shown here

        //Example
        HashMap<String,String> cache= new HashMap<>();
        cache.put("name","saurabh");
        Observable.create(s -> {
            s.onNext(cache.get("name"));
            s.onComplete();
        }).subscribe(value -> System.out.println(value));


        //This scheduling choice is powerful when the data might or might not be in memory.
        //If it is in memory, emit it synchronously; if it’s not, perform the network call asyn‐
        //chronously and return the data when it arrives. This choice can reside conditionally
        //within the Observable :

        // pseudo-code
        /*Observable.create(s -> {
            String fromCache = getFromCache("key");
            if(fromCache != null) {
            // emit synchronously
                s.onNext(fromCache);
                s.onComplete();
            } else {
            // fetch asynchronously
                getDataAsynchronously(SOME_KEY)
                        .onResponse(v -> {
                            putInCache(SOME_KEY, v);
                            s.onNext(v);
                            s.onCompleted();
                        })
                        .onFailure(exception -> {
                            s.onError(exception);
                        });
            }
        }).subscribe(s -> System.out.println(s));*/


        //Synchronous computation (such as operators)
        //The more common reason for remaining synchronous is stream composition and
        //transformation via operators. RxJava mostly uses the large API of operators used to
        //manipulate, combine, and transform data, such as map() , filter() , take() , flat
        //Map() , and groupBy() . Most of these operators are synchronous, meaning that they
        //perform their computation synchronously inside the onNext() as the events pass by.

        //These operators are synchronous for performance reasons. Take this as an example:
        Observable<Integer> o = Observable.create(s -> {
            s.onNext(1);
            s.onNext(2);
            s.onNext(3);
            s.onComplete();
        });
        o.map(i -> "Number " + i)
                .subscribe(s -> System.out.println(s));
        //If the map operator defaulted to being asynchronous, each number (1, 2, 3) would be
        //scheduled onto a thread where the string concatenation would be performed (“Num‐
        //ber " + i). This is very inefficient and generally has nondeterministic latency due to
        //scheduling, context switching, and so on.
        //The important thing to understand here is that most Observable function pipelines
        //are synchronous (unless a specific operator needs to be async, such as timeout or
        //observeOn ), whereas the Observable itself can be async.

        //The contract of an RxJava Observable is that events ( onNext() , onCompleted() , onEr
        //ror() ) can never be emitted concurrently. In other words, a single Observable
        //stream must always be serialized and thread-safe. Each event can be emitted from a
        //different thread, as long as the emissions are not concurrent. This means no inter‐
        //leaving or simultaneous execution of onNext() . If onNext() is still being executed on
        //one thread, another thread cannot begin invoking it again (interleaving).
        //Here’s an example of what’s okay:
        Observable.create(s -> {
            new Thread(() -> {
                s.onNext("one");
                s.onNext("two");
                s.onNext("three");
                s.onNext("four");
                s.onComplete();
            }).start();
        });

        //This code emits data sequentially, so it complies with the contract. (Note, however,
        //that it is generally advised to not start a thread like that inside an Observable . Use
        //schedulers, instead)

        //Here’s an example of code that is illegal:
        // DO NOT DO THIS
        Observable.create(s -> {
            // Thread A
            new Thread(() -> {
                s.onNext("one");
                s.onNext("two");
            }).start();
            // Thread B
            new Thread(() -> {
                s.onNext("three");
                s.onNext("four");
            }).start();
            // ignoring need to emit s.onCompleted() due to race of threads
        });
        // DO NOT DO THIS
        //This code is illegal because it has two threads that can both invoke onNext() concur‐
        //rently. This breaks the contract. (Also, it would need to safely wait for both threads to
        //complete to call onComplete , and as mentioned earlier, it is generally a bad idea to
        //manually start threads like this.)

        //So, how do you take advantage of concurrency and/or parallelism with RxJava? Com‐
        //position.

        //A single Observable stream is always serialized, but each Observable stream can
        //operate independently of one another, and thus concurrently and/or in parallel. This
        //is why merge and flatMap end up being so commonly used in RxJava—to compose
        //asynchronous streams together concurrently.

        //Here is a contrived example showing the mechanics of two asynchronous Observables running on separate threads and merged together:

        Observable<String> a = Observable.create(s -> {
            new Thread(() -> {
                s.onNext("one");
                s.onNext("two");
                s.onComplete();
            }).start();
        });
        Observable<String> b = Observable.create(s -> {
            new Thread(() -> {
                s.onNext("three");
                s.onNext("four");
                s.onComplete();
            }).start();
        });
        // this subscribes to a and b concurrently,
        // and merges into a third sequential stream
        Observable<String> c = Observable.merge(a, b);


    }
}
