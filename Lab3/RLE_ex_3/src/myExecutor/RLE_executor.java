package myExecutor;


import ru.spbstu.pipeline.Consumer;
import ru.spbstu.pipeline.Executor;
import ru.spbstu.pipeline.Producer;
import ru.spbstu.pipeline.Status;
import ru.spbstu.pipeline.logging.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class RLE_executor implements Executor {
    private byte[] workArr;
    private final Logger logger;
    private Status status = Status.OK;
    private ConfigParser configs;
    private String dataType = byte[].class.getCanonicalName();
    private List<Producer> producers = new ArrayList<>();
    private List<Consumer> consumers = new ArrayList<>();

    private Map<String, DataAccessor> availableAccessors = new HashMap<>();
    private Map<Producer, DataAccessor> prodAccessorsMap = new HashMap<>();

    private final DataAccessor ByteAcc = new ByteDataAccessor();
    private final DataAccessor CharAcc = new CharDataAccessor();
    private final DataAccessor StringAcc = new StringDataAccessor();



    private void initAvailAcc(){
        availableAccessors.put(byte[].class.getCanonicalName(), ByteAcc);
        availableAccessors.put(String.class.getCanonicalName(), CharAcc);
        availableAccessors.put(char[].class.getCanonicalName(), StringAcc);
    }

    private byte[] ConvertToByteArr(Object data) {
        if (dataType.equals(byte[].class.getCanonicalName())) {
            return (byte[]) data;
        }
        else if (dataType.equals(char[].class.getCanonicalName())) {
            return new String((char[]) data).getBytes(StandardCharsets.UTF_16BE);
        }
        else {
            return ((String) data).getBytes();
        }
    }

    @Override
    public long loadDataFrom(Producer producer) {
        DataAccessor dAcc = prodAccessorsMap.get(producer);
        Object data = dAcc.get();
        workArr = ConvertToByteArr(data);
        return workArr.length;
    }

    @Override
    public void run() {
        ExecErrors err = compression();
        if (err != ExecErrors.NO_ERRORS) {
            status = Status.EXECUTOR_ERROR;
            logger.log("Error occurred while running RLE_executor : " + err.GetDescription());
        }
        long size = consumers.get(0).loadDataFrom(this);
        if (size == 0) {
            status = Status.EXECUTOR_ERROR;
            return;
        }
        consumers.get(0).run();
        this.status = consumers.get(0).status();
    }

    @Override
    public void addProducer(Producer producer) {
        if (producer == null)
            return;
        producers.add(producer);
        Set<String> dataTypes = producer.outputDataTypes();
        if (dataTypes.contains(dataType)) {
            prodAccessorsMap.put(producer, producer.getAccessor(dataType));
        }
        else {
            status = Status.ERROR;
            logger.log("No possible compassion between RLE_exec and his producer.");
        }
    }

    @Override
    public void addProducers(List<Producer> producers) {
        for (Producer prod: producers) {
            addProducer(prod);
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

    @Override
    public Status status() {
        return status;
    }



    public RLE_executor(String confPath, Logger logger) {
        this.logger = logger;
        configs = new ConfigParser();
        ExecErrors err = configs.parse(confPath);
        if (err != ExecErrors.NO_ERRORS) {
            logger.log("Error occurred during creation of RLE_executor:" + err.GetDescription());
            status = Status.EXECUTOR_ERROR;
        }
        initAvailAcc();
    }

    public RLE_executor(String confPath, Logger logger, String dataType) {
        this.logger = logger;
        configs = new ConfigParser();
        this.dataType = dataType;
        ExecErrors err = configs.parse(confPath);
        if (err != ExecErrors.NO_ERRORS) {
            logger.log("Error occurred during creation of RLE_executor:" + err.GetDescription());
            status = Status.EXECUTOR_ERROR;
        }
        initAvailAcc();
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

    @Override
    public DataAccessor getAccessor(String s) {
        /*String accName = availableAccessors.get(s);
        try {
            Class c = Class.forName(accName);
            return (DataAccessor) c.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            logger.log("Error occurred during matching RLE_executor:", e);
            status = Status.EXECUTOR_ERROR;
        }
        return null;*/
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
