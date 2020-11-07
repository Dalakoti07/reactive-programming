package books.apress.chapter4;

import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.List;

public class Flowables {
    public static List doSquares() {
        List squares = new ArrayList();
        Flowable.range(1, 64) //1
                .observeOn(Schedulers.computation()) //2
                .map(v -> v * v) //3
                .blockingSubscribe(squares::add); //4
        //1. Create a range from 1 to 64.

        //2. Call the method observeOn to determine which
        //Scheduler to use. This determines on which Thread
        //or Threads the flow will run. The Scheduler returned
        //from “computation()” takes advantage of all
        //available processors when possible.

        //3. The map method transforms each value. In this case
        //we calculate the square.

        //4. Finally, we initiate the flow by calling a “subscribe”
        //method. In this case, blockingSubscribe blocks
        //until the entire flow has completed, and we add
        //each value to the “squares” List. This means that
        //the squares list will be populated before the return
        //statement. Otherwise the flow would run on a
        //different thread and the values in the squares list
        //would be unpredictable at any given time.
        return squares;
    }
}
