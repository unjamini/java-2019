package com.company;
import ru.spbstu.pipeline.*;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.*;
import java.util.List;


public class Writer implements  ru.spbstu.pipeline.Writer {


    private BufferedOutputStream outStream;
    private byte[] workArr;
    private List<Producer> producers = new ArrayList<>();
    private Status status = Status.OK;
    private final Logger logger;

    @Override
    public void loadDataFrom(Producer producer) {
        status = producer.status();
        if (status == Status.OK) {
            workArr = (byte[]) producer.get();
        }
    }

    @Override
    public void run() {
        if (status == Status.OK) {
            if (WriteToFile() != InternalStatus.OK)
            {
                status = Status.WRITER_ERROR;
            }

        }
    }

    @Override
    public Status status() {
        return status;
    }

    @Override
    public void addProducer(Producer producer) {
        producers.add(producer);
    }

    @Override
    public void addProducers(List<Producer> producers) {
        this.producers = producers;
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


//что делать если writer получает статус ошибки???

