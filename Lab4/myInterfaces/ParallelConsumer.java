package myInterfaces;

import ru.spbstu.pipeline.Consumer;

public interface ParallelConsumer extends Consumer {
    boolean readyToGet();
    int requiresLen();
    long loadDataFrom(ParallelProducer producer);
    void addProducer(ParallelProducer producer);
}
