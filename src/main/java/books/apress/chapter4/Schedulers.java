package books.apress.chapter4;

import io.reactivex.Flowable;

public class Schedulers {
    public static void runComputation(){
        Flowable<String> source = Flowable.fromCallable(
                () -> { //1
                    Thread.sleep(1000);
                    return "Done";
                });
        source.doOnComplete(
                    () ->  System.out.println("Completed runComputation")
                );

        Flowable<String> background =source.subscribeOn(io.reactivex.schedulers.Schedulers.io()); //2
        Flowable<String> foreground = background.observeOn(io.reactivex.schedulers.Schedulers.single()); //3
        foreground.subscribe(System.out::println,
                Throwable::printStackTrace); //4

        //1. Create a new Flowable from a Callable (functional
        //interface (SAM) which simply returns a value).
        //2. Run the Flowable using the “IO” Scheduler. This
        //Scheduler uses a cached thread pool which is good
        //for I/O (e.g., reading and writing to disk or network
        //transfers).
        //3. Observe the results of the Flowable using a single-
        //threaded Scheduler.
        //4. Finally, subscribe to the resulting foreground
        //Flowable to initiate the flow and print the results to
        //standard out. The result of calling runComputation()
        //will be “Done” printed after one second.
    }

    public static void main(String[] args) {
        runComputation();
    }
}
