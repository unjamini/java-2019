package com.company;

import ru.spbstu.pipeline.Producer;
import ru.spbstu.pipeline.Status;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Writer implements  ru.spbstu.pipeline.Writer {


    private BufferedOutputStream outStream;
    private byte[] workArr;
    private List<Producer> producers = new ArrayList<>();
    private Status status = Status.OK;
    private final static String inputType = byte[].class.getCanonicalName();
    private final Logger logger;
    private Map<Producer, Producer.DataAccessor> prodAccessorsMap = new HashMap<>();


    @Override
    public long loadDataFrom(Producer producer) {
        Producer.DataAccessor dAcc = prodAccessorsMap.get(producer);
        Object data = dAcc.get();
        workArr = (byte[]) data;
        return workArr.length;
    }

    @Override
    public void run() {
        if (WriteToFile() != InternalStatus.OK) {
            status = Status.WRITER_ERROR;
        }
    }

    @Override
    public Status status() {
        return status;
    }

    @Override
    public void addProducer(Producer producer) {
        if (producer == null)
            return;
        producers.add(producer);
        Set<String> dataTypes = producer.outputDataTypes();
        if (dataTypes.contains(inputType)) {
            prodAccessorsMap.put(producer, producer.getAccessor(inputType));
        }
        else {
            status = Status.ERROR;
            logger.log(Level.INFO, "No possible compassion between Writer and his producer.");
        }
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
            outStream.write(workArr);
            outStream.flush();
        }
        catch (IOException ex) {
            logger.log(Level.INFO, "Writer exception: ", ex);
            return InternalStatus.ERROR;
        }
        return InternalStatus.OK;
    }
}


