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



#### Note
Lazy loading is heart of functional programming



