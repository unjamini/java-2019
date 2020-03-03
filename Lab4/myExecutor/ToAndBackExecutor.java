package myExecutor;

import javafx.util.Pair;
import myInterfaces.ParallelConsumer;
import myInterfaces.ParallelExecutor;
import myInterfaces.ParallelProducer;
import ru.spbstu.pipeline.Consumer;
import ru.spbstu.pipeline.Producer;
import ru.spbstu.pipeline.Status;
import ru.spbstu.pipeline.logging.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ToAndBackExecutor implements ParallelExecutor {
    private byte[] workArr;
    private final Logger logger;
    private Status status = Status.OK;
    private ConfigParser configs;
    private String dataType = byte[].class.getCanonicalName();
    private ParallelProducer producer;
    private ParallelConsumer consumer;

    private Map<String, ParallelAccessor> availableAccessors = new HashMap<>();
    private Map<ParallelProducer, ParallelAccessor> prodAccessorsMap = new HashMap<>();

    private final ParallelAccessor ByteAcc = new myExecutor.ToAndBackExecutor.ByteDataAccessor();
    private final ParallelAccessor CharAcc = new myExecutor.ToAndBackExecutor.CharDataAccessor();
    private final ParallelAccessor StringAcc = new myExecutor.ToAndBackExecutor.StringDataAccessor();
    private final Charset charset = StandardCharsets.UTF_8;

    private boolean readyWork = false;
    private boolean readyGet = true;
    private int shifted = 0;
    private Map<ParallelConsumer, Pair<Integer, Integer>> consumerLenMap = new HashMap<>();


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
            return new String((char[]) data).getBytes(charset);
        }
        else {
            return ((String) data).getBytes();
        }
    }

    @Override
    public long loadDataFrom(ParallelProducer producer) {
        readyGet = false;
        ParallelAccessor dAcc = prodAccessorsMap.get(producer);
        Object data = dAcc.get(this);
        workArr = ConvertToByteArr(data);
        readyWork = true;
        return workArr.length;
    }


    @Override
    public void run() {
        while (!Thread.interrupted() && this.status == Status.OK) {
            if (readyWork && allConsReady()) {
                readyWork = false;
                ExecErrors err = compression();
                if (err != ExecErrors.NO_ERRORS) {
                    status = Status.EXECUTOR_ERROR;
                    logger.log("Error occurred while running RLE_executor : " + err.GetDescription());
                }
                long size = 1;
                for (ParallelConsumer cons: consumers)
                    size *= cons.loadDataFrom(this);
                if (size == 0) {
                    status = Status.EXECUTOR_ERROR;
                } else {
                    readyGet = true;
                }
            }
        }
    }

    @Override
    public void addProducer(ParallelProducer producer) {
        if (producer == null)
            return;
        producers.add(producer);
        Set<String> dataTypes = producer.outputDataTypes();
        if (dataTypes.contains(dataType)) {
            prodAccessorsMap.put(producer, producer.getParAccessor(dataType));
        }
        else {
            status = Status.ERROR;
            logger.log("No possible compassion between RLE_exec and his producer.");
        }
    }


    @Override
    public void addProducer(Producer producer) {
        System.out.println("Wrong add prod!!!!!");
    }

    @Override
    public void addProducers(List<Producer> producers) {
        for (Producer prod: producers) {
            addProducer(prod);
        }
    }

    @Override
    public void addConsumer(Consumer consumer) {
        System.out.println("Wrong addCons!!!!");
    }


    @Override
    public void addConsumer(ParallelConsumer consumer) {
        consumers.add(consumer);
    }


    @Override
    public void addConsumers(List<Consumer> consumers) {
        for (Consumer cons: consumers) {
            addConsumer(cons);
        }

    }

    @Override
    public Status status() {
        return status;
    }



    public ToAndBackExecutor(String confPath, Logger logger) {
        this.logger = logger;
        configs = new ConfigParser();
        ExecErrors err = configs.parse(confPath);
        if (err != ExecErrors.NO_ERRORS) {
            logger.log("Error occurred during creation of RLE_executor:" + err.GetDescription());
            status = Status.EXECUTOR_ERROR;
        }
        initAvailAcc();
    }

    public ToAndBackExecutor(String confPath, Logger logger, String dataType) {
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
        err = Processing.ToBack(configs, inStr, outStr);
        if (err == ExecErrors.NO_ERRORS) {
            workArr = out.toByteArray();
        }
        return err;
    }




    public ParallelAccessor getParAccessor(String s) {
        return availableAccessors.get(s);
    }


    @Override
    public Set<String> outputDataTypes() {
        return availableAccessors.keySet();
    }


    @Override
    public long loadDataFrom(Producer producer) {
        System.out.println("Wrong load!!!!!");
        return 0;
    }

    @Override
    public DataAccessor getAccessor(String s) {
        System.out.println("Wrong get acc!!!!!");
        return null;
    }

    @Override
    public boolean readyToGet() {
        return readyGet;
    }

    @Override
    public int requiresLen() {
        return configs.getBufferLimit();
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
            return workArr;
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
            return new String(workArr, charset);
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
            String str = new String(workArr, charset);
            return str.toCharArray();
        }
    }


    private boolean allConsReady(){
        return consumer.readyToGet();
    }
}
