package books.apress.chapter4;


public class mainDriver {

    public static void main(String[] args) {
        System.out.println("flowable result"+Flowables.doSquares());

        System.out.println("Parallel computing results "+ParallelComputing.doParallelSquares());

        System.out.println("Schedulers on IO ");
    }
}
