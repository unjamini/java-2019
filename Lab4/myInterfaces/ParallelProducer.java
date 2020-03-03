package myInterfaces;

import ru.spbstu.pipeline.Producer;

public interface ParallelProducer extends Producer {

    public interface ParallelAccessor extends Producer.DataAccessor {
        public Object get(ParallelConsumer consumer);
    }

    void addConsumer(ParallelConsumer consumer);
    ParallelAccessor getParAccessor(String s);
}

