package myInterfaces;

import ru.spbstu.pipeline.Reader;

public interface ParallelReader extends Reader, ParallelProducer{
    boolean reachedEOF();
}
