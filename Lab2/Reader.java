package com.company;
import ru.spbstu.pipeline.*;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


public class Reader implements ru.spbstu.pipeline.Reader {
    private BufferedInputStream inStream;
    private Writer writer;
    private int bufferLimit;
    private Logger logger;
    private List<Consumer> consumers = new ArrayList<>();
    private Status status = Status.OK;
    private byte[] workArr;


    @Override
    public void run() {
        while (ReadFromFile() != InternalStatus.EOF && writer.status() == Status.OK) {
            consumers.get(0).loadDataFrom(this);
            consumers.get(0).run();
        }
        status = writer.status();
    }

    @Override
    public Object get() {
        return workArr;
    }

    @Override
    public void addConsumer(Consumer consumer) {
        consumers.add(consumer);
    }

    @Override
    public void addConsumers(List<Consumer> consumers) {
        this.consumers = consumers;
    }

    @Override
    public Status status() {
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

}
