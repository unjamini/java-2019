package com.company;

import javafx.util.Pair;
import myInterfaces.ParallelConsumer;
import myInterfaces.ParallelProducer;
import myInterfaces.ParallelReader;
import ru.spbstu.pipeline.Consumer;
import ru.spbstu.pipeline.Producer;
import ru.spbstu.pipeline.Status;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Math.min;


public class Reader implements ParallelReader {
    private BufferedInputStream inStream;
    private Writer writer;
    private int bufferLimit;
    private Logger logger;
    private List<ParallelConsumer> consumers = new ArrayList<>();
    private Status status = Status.OK;
    private byte[] workArr;

    private static Map<String, ParallelAccessor> availableAccessors = new HashMap<>();
    private final ParallelAccessor ByteAcc = new ByteDataAccessor();
    private final ParallelAccessor CharAcc = new CharDataAccessor();
    private final ParallelAccessor StringAcc = new StringDataAccessor();
    private final Charset charset = StandardCharsets.UTF_8;

    private int shifted = 0;
    private boolean reachedEOF = false;

    private void initAvailAcc(){
        availableAccessors.put(byte[].class.getCanonicalName(), ByteAcc);
        availableAccessors.put(String.class.getCanonicalName(), CharAcc);
        availableAccessors.put(char[].class.getCanonicalName(), StringAcc);
    }

    @Override
    public void run() {
        while (!Thread.interrupted() && !reachedEOF && this.status == Status.OK) {
            if (allConsReady()) {
                if (ReadFromFile() == InternalStatus.EOF) {
                    reachedEOF = true;
                }
                else {
                    long size = 1;
                    for (ParallelConsumer cons: consumers)
                        size *= cons.loadDataFrom(this);
                    if (size == 0) {
                        status = Status.EXECUTOR_ERROR;
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void addConsumer(ParallelConsumer consumer) {
        consumers.add(consumer);
    }



    Status status() {
        return status;
    }

    Reader(String filename, Logger logger, Writer writer, int bufferLimit)
    {
        this.writer = writer;
        this.bufferLimit = bufferLimit;
        this.logger = logger;
        try {
            FileInputStream in = new FileInputStream(filename);
            inStream = new BufferedInputStream(in, bufferLimit);
        }
        catch (FileNotFoundException ex) {
            logger.log(Level.INFO,"Not possible to open input file.");
            status = Status.READER_ERROR;
        }
        initAvailAcc();
        System.out.println("Reader created");
    }


    private InternalStatus ReadFromFile () {
        byte[] buff = new byte[bufferLimit];
        int bytesRead;
        try {
            bytesRead = inStream.read(buff);
            if (bytesRead == -1) {
                return InternalStatus.EOF;
            }
            workArr = Arrays.copyOfRange(buff, 0, bytesRead);
        }
        catch (IOException ex) {
            logger.log(Level.INFO,"Not possible to read from file.");
            return InternalStatus.ERROR;
        }
        return InternalStatus.OK;
    }

    @Override
    public DataAccessor getAccessor(String s) {
        return null;
    }



    @Override
    public ParallelAccessor getParAccessor(String s) {
        return availableAccessors.get(s);
    }


    public Set<String> outputDataTypes() {
        return availableAccessors.keySet();
    }

    @Override
    public boolean reachedEOF() {
        return reachedEOF;
    }


    private class ByteDataAccessor implements ParallelProducer.ParallelAccessor {
        @Override
        public Object get() {
            return workArr;
        }

        @Override
        public long size() {
            return workArr.length;
        }

        @Override
        public Object get(ParallelConsumer consumer) {
            int idx = consumers.indexOf(consumer);
            int numberCons = consumers.size();
            int len = bufferLimit / numberCons;
            byte[] arr = Arrays.copyOfRange(workArr, idx * len, min((idx + 1) * len, workArr.length));
            return arr;
        }
    }


    private class StringDataAccessor implements ParallelProducer.ParallelAccessor {

        @Override
        public Object get() {
            return new String(workArr, charset);
        }

        @Override
        public long size() {
            return workArr.length;
        }

        @Override
        public Object get(ParallelConsumer consumer) {
            int idx = consumers.indexOf(consumer);
            int numberCons = consumers.size();
            int len = bufferLimit / numberCons;
            byte[] arr = Arrays.copyOfRange(workArr, idx * len, min((idx + 1) * len, workArr.length));
            return new String(arr, charset);
        }
    }

    private class CharDataAccessor implements ParallelProducer.ParallelAccessor {

        @Override
        public Object get() {
            String str = new String(workArr, charset);
            return str.toCharArray();
        }

        @Override
        public long size() {
            return workArr.length;
        }

        @Override
        public Object get(ParallelConsumer consumer) {
            int idx = consumers.indexOf(consumer);
            int numberCons = consumers.size();
            int len = bufferLimit / numberCons;
            byte[] arr = Arrays.copyOfRange(workArr, idx * len, min((idx + 1) * len, workArr.length));
            return new String(arr, charset).toCharArray();
        }
    }


    private boolean allConsReady(){
        for (ParallelConsumer cons: consumers) {
            boolean b = cons.readyToGet();
            if (!b) {
                return false;
            }
        }
        return true;
    }


    @Override
    public void addConsumers(List<Consumer> consumers) {
        for (Consumer cons: consumers) {
            addConsumer(cons);
        }
    }

    @Override
    public void addConsumer(Consumer consumer) {}
}
