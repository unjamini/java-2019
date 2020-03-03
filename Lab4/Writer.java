package com.company;

import myInterfaces.ParallelProducer;
import myInterfaces.ParallelWriter;
import ru.spbstu.pipeline.Producer;
import ru.spbstu.pipeline.Status;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Writer implements ParallelWriter {
    private BufferedOutputStream outStream;
    private Map<ParallelProducer, String> workArr = new HashMap<>();
    private List<ParallelProducer> producers = new ArrayList<>();
    private Status status = Status.OK;
    private final static String inputType = byte[].class.getCanonicalName();
    private final Logger logger;
    private Map<ParallelProducer, ParallelProducer.ParallelAccessor> prodAccessorsMap = new HashMap<>();

    private boolean readyGet = true;
    private boolean readyWrite = false;
    private int alreadyLoadedInfo = 0;


    public long loadDataFrom(ParallelProducer producer) {
        readyGet = false;
        alreadyLoadedInfo++;
        if (alreadyLoadedInfo != producers.size())
            readyGet = true;
        ParallelProducer.ParallelAccessor dAcc = prodAccessorsMap.get(producer);
        Object data = dAcc.get();
        workArr.put(producer, new String((byte[]) data));
        return workArr.get(producer).toCharArray().length;
    }

    @Override
    public long loadDataFrom(Producer producer) {
        return 0;
    }

    @Override
    public void run() {
        while (!Thread.interrupted() && this.status == Status.OK) {
            checkReadyWrite();
            if (readyWrite) {
                readyWrite = false;
                if (WriteToFile() != InternalStatus.OK) {
                    status = Status.WRITER_ERROR;
                } else {
                    readyGet = true;
                }
            }
        }

    }

    private void checkReadyWrite() {
        if (alreadyLoadedInfo == producers.size()) {
            alreadyLoadedInfo = 0;
            readyWrite = true;
        }
    }

    @Override
    public Status status() {
        return status;
    }

    public void addProducer(ParallelProducer producer) {
        if (producer == null)
            return;
        producers.add(producer);
        Set<String> dataTypes = producer.outputDataTypes();
        if (dataTypes.contains(inputType)) {
            prodAccessorsMap.put(producer, producer.getParAccessor(inputType));
        }
        else {
            status = Status.ERROR;
            logger.log(Level.INFO, "No possible compassion between Writer and his producer.");
        }
    }


    @Override
    public void addProducer(Producer producer) {

    }

    @Override
    public void addProducers(List<Producer> producers) {
        for (Producer prod: producers) {
            addProducer(prod);
        }
    }


    Writer(String filename, Logger logger) {
        this.logger = logger;
        try{
            FileOutputStream out = new FileOutputStream(filename);
            outStream = new BufferedOutputStream(out);
        }
        catch (FileNotFoundException ex) {
            logger.log(Level.INFO,"Not possible to open the file.");
            status = Status.WRITER_ERROR;
        }
        System.out.println("Writer created");
    }


    private InternalStatus WriteToFile() {
        try {
            for (ParallelProducer prod: producers) {
                byte[] arr = workArr.get(prod).getBytes();
                outStream.write(arr);
                outStream.flush();
            }
        }
        catch (IOException ex) {
            logger.log(Level.INFO, "Writer exception: ", ex);
            return InternalStatus.ERROR;
        }
        return InternalStatus.OK;
    }

    @Override
    public boolean readyToGet() {
        return readyGet;
    }

    @Override
    public int requiresLen() {
        return 0;
    }
}


