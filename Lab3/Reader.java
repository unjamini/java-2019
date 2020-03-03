package com.company;

import ru.spbstu.pipeline.Consumer;
import ru.spbstu.pipeline.Producer;
import ru.spbstu.pipeline.Status;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Reader implements ru.spbstu.pipeline.Reader {
    private BufferedInputStream inStream;
    private Writer writer;
    private int bufferLimit;
    private Logger logger;
    private List<Consumer> consumers = new ArrayList<>();
    private Status status = Status.OK;
    private byte[] workArr;

    private static Map<String, DataAccessor> availableAccessors = new HashMap<>();
    private final DataAccessor ByteAcc = new ByteDataAccessor();
    private final DataAccessor CharAcc = new CharDataAccessor();
    private final DataAccessor StringAcc = new StringDataAccessor();


    private void initAvailAcc(){
        availableAccessors.put(byte[].class.getCanonicalName(), ByteAcc);
        availableAccessors.put(String.class.getCanonicalName(), CharAcc);
        availableAccessors.put(char[].class.getCanonicalName(), StringAcc);
    }

    @Override
    public void run() {
        while (ReadFromFile() != InternalStatus.EOF && this.status == Status.OK) {
            long size = consumers.get(0).loadDataFrom(this);
            if (size == 0) {
                status = Status.EXECUTOR_ERROR;
                return;
            }
            consumers.get(0).run();
            this.status = consumers.get(0).status();
        }
    }


    @Override
    public void addConsumer(Consumer consumer) {
        consumers.add(consumer);
    }

    @Override
    public void addConsumers(List<Consumer> consumers) {
        this.consumers = consumers;
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
        return availableAccessors.get(s);
    }


    public Set<String> outputDataTypes() {
        return availableAccessors.keySet();
    }


    private class ByteDataAccessor implements Producer.DataAccessor {

        @Override
        public Object get() {
            return workArr;
        }

        @Override
        public long size() {
            return workArr.length;
        }
    }

    private class StringDataAccessor implements Producer.DataAccessor {

        @Override
        public Object get() {
            return new String(workArr, StandardCharsets.UTF_16BE);
        }

        @Override
        public long size() {
            return workArr.length;
        }
    }

    private class CharDataAccessor implements Producer.DataAccessor {

        @Override
        public Object get() {
            String str = new String(workArr, StandardCharsets.UTF_16BE);
            return str.toCharArray();
        }

        @Override
        public long size() {
            return workArr.length;
        }
    }
}
