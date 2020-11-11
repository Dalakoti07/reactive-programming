### Learnings

- Observables are lazy by default
- traditional blocking programs and the one with Observable work exactly the
    same way
- Two types of observable - Hot and Cold
- A good rule of thumb is that whenever you see double-
  wrapped type (for example Optional<Optional<...>> ) there is a flatMap() invocation missing somewhere.
- A callback API not equal to stream API


### Remaining
- chapter 5,6 and 7