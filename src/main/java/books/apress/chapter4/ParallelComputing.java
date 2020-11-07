package books.apress.chapter4;

import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.List;

public class ParallelComputing {
    public static List doParallelSquares() {
        //If you tie a Flowable to one Scheduler as in the flowable example, it would
        //run in succession, not in parallel. To run each calculation in parallel, you
        //could use flatMap to break out each calculation into a separate Flowable
        //as follows:
        List squares = new ArrayList();
        Flowable.range(1, 64)
                .flatMap(v -> //1
                        Flowable.just(v)
                                .subscribeOn(Schedulers.computation())
                                .map(w -> w * w)
                )
                .doOnError(ex -> ex.printStackTrace()) //2
                .doOnComplete(() ->
                        System.out.println("Completed")) //3
                .blockingSubscribe(squares::add);
        return squares;
        //1. Call flatMap with a lambda expression that takes in
        //a value and returns another Flowable. The Flowable.
        //just(...) method takes in any number of objects and
        //returns a Flowable that will emit those objects and
        //then complete.
        //2. Call doOnError to handle errors that occur.
        //3. Call doOnComplete to execute something after a
        //Flowable has completed. This is only possible for
        //Flowables that have clear endings, such as ranges. The
        //resulting List will have the same values as the previous
        //example, but since we used flatMap, the resulting
        //values will not necessarily be in the same order.
    }
}
