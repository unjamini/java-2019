package myExecutor;


import ru.spbstu.pipeline.*;
import ru.spbstu.pipeline.logging.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;


public class RLE_executor implements Executor {
    private byte[] workArr;
    private final Logger logger;
    private Status status = Status.OK;
    private ConfigParser configs;
    private List<Producer> producers = new ArrayList<>();
    private List<Consumer> consumers = new ArrayList<>();


    @Override
    public void loadDataFrom(Producer producer) {
        if (producer.status() == Status.OK) {
            workArr = (byte[]) producer.get();
        }
        else {
            status = producer.status();
        }
    }

    @Override
    public void run() {
        if (status == Status.OK) {
            ExecErrors err = compression();
            if (err != ExecErrors.NO_ERRORS) {
                status = Status.ERROR;
                logger.log("Error occurred while creating RLE_executor : " + err.GetDescription());
            }
        }
        consumers.get(0).loadDataFrom(this);
        consumers.get(0).run();
    }

    @Override
    public void addProducer(Producer producer) {
        producers.add(producer);
    }

    @Override
    public void addProducers(List<Producer> producers) {
        this.producers = producers;
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



    public RLE_executor(String confPath, Logger logger) {
        this.logger = logger;
        configs = new ConfigParser();
        ExecErrors err = configs.parse(confPath);
        if (err != ExecErrors.NO_ERRORS) {
            logger.log("Error occured during work of RLE_executor:" + err.GetDescription());
            status = Status.EXECUTOR_ERROR;
        }
    }


    private ExecErrors compression() {
        ExecErrors err;
        int bufferLim = workArr.length;
        ByteArrayInputStream in = new ByteArrayInputStream(workArr);
        ByteArrayOutputStream out = new ByteArrayOutputStream(bufferLim);
        BufferedInputStream inStr = new BufferedInputStream(in, bufferLim);
        BufferedOutputStream outStr = new BufferedOutputStream(out);
        if (configs.getMode()) {
            err = Processing.DecompressFile(configs, inStr, outStr);
        }
        else {
            err = Processing.CompressFile(configs, inStr, outStr);
        }
        if (err == ExecErrors.NO_ERRORS) {
            workArr = out.toByteArray();
        }
        return err;
    }


}
