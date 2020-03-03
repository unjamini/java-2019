package com.company;

import ru.spbstu.pipeline.Executor;
import ru.spbstu.pipeline.Producer;
import ru.spbstu.pipeline.Status;
import ru.spbstu.pipeline.logging.UtilLogger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


public class Manager {

    private static final String DELIMITER = "=";
    private static final String INFO_DELIMITER = " ";
    private static final String EXEC_PREFIX = "EX";
    private static final int NUMBER_OF_PARAMS_EX = 2;
    private static final int CLASSNAME = 0;
    private static final int CONFIG_PATH = 1;

    public enum Grammar {
        FILE_IN ("FILE IN"),
        FILE_OUT ("FILE OUT"),
        BUFFER_LIMIT("LIMIT"),
        EXEC_NUMBER ("EXECUTORS NUMBER");


        private String title;

        Grammar(String title) {
            this.title = title;
        }

        public String getTitle(){ return title;}
    }

    private String inputFilename;
    private String outputFilename;
    private int bufferLimit;
    private int execNumber;
    private List<Executor> exList = new ArrayList<>();

    private Reader reader;
    private Writer writer;
    private Logger logger;
    private Status status;


    public Status getStatus(){
        return this.status;
    }

    public void Log(String str) {
        logger.log(Level.INFO, str);
    }

    public ErrorsHandle InitLogging (String logPath) {
        logger = Logger.getLogger(Manager.class.getName());
        FileHandler fileH;
        try {
            logger.setUseParentHandlers(false);
            fileH = new FileHandler(logPath, true);
            SimpleFormatter formatter = new SimpleFormatter();
            fileH.setFormatter(formatter);
            logger.addHandler(fileH);
        } catch (IOException e) {
            e.printStackTrace();
            return ErrorsHandle.LOG_ERROR;
        }
        return ErrorsHandle.NO_ERRORS;
    }


    public ErrorsHandle ReadConfig (String configFilename) {
        List<String> lines = new ArrayList<String>();
        try {
            FileReader configReader = new FileReader(configFilename);
            BufferedReader bufferedReader = new BufferedReader(configReader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                lines.add(line);
            }
            bufferedReader.close();
            configReader.close();
        }
        catch (IOException e) {
            logger.log(Level.INFO, ErrorsHandle.CONFIG_READ.GetDescription());
            return ErrorsHandle.CONFIG_READ;
        }

        Grammar[] grammar = Grammar.values();
        for (Grammar param: grammar) {
            boolean paramSet = false;
            for (String curLine: lines) {
                if (curLine.contains(param.getTitle())) {
                    String data = curLine.substring(curLine.indexOf(param.getTitle()) + param.getTitle().length()).trim();
                    if (data.contains(DELIMITER)) {
                        data = data.substring(data.indexOf(DELIMITER) + DELIMITER.length()).trim();
                    }
                    switch (param) {
                        case FILE_IN:
                            inputFilename = data;
                            break;
                        case FILE_OUT:
                            outputFilename = data;
                            break;
                        case BUFFER_LIMIT:
                            bufferLimit = Integer.parseInt(data);
                            break;
                        case EXEC_NUMBER:
                            execNumber = Integer.parseInt(data);
                            if (execNumber < 0) {
                                ErrorsHandle err = ErrorsHandle.CONFIG_READ;
                                err.AddInformation("Wrong number of executors: " + execNumber);
                                logger.log(Level.INFO, err.GetDescription());
                                return err;
                            }
                            break;
                    }
                    paramSet = true;
                    break;
                }
            }
            if (!paramSet) {
                ErrorsHandle err = ErrorsHandle.CONFIG_ARGS;
                err.AddInformation(param.getTitle());
                logger.log(Level.INFO, err.GetDescription());
                return err;
            }
        }
        writer = new Writer(outputFilename, logger);
        reader = new Reader(inputFilename, logger, writer, bufferLimit);
        if (reader.status() != Status.OK ) {
            logger.log(Level.INFO, "Problems with reading.");
            return ErrorsHandle.INPUT_READ;
        }

        Executor producer = null;
        for (int exCount = 1; exCount <= execNumber; exCount++) {
            boolean paramSet = false;
            String exName = EXEC_PREFIX + exCount;
            for (String curLine: lines) {
                if (curLine.contains(exName)) {
                    System.out.println(exName);
                    String exInfo = curLine.substring(curLine.indexOf(exName) + exName.length()).trim();
                    String[] spltInfo = exInfo.split(INFO_DELIMITER);
                    if (spltInfo.length != NUMBER_OF_PARAMS_EX) {
                        logger.log(Level.INFO, "Not enough arguments for " + exName);
                        return ErrorsHandle.CONFIG_ARGS;
                    }
                    ErrorsHandle err;
                    if (producer != null)
                        err = CreateExecutor(spltInfo[CLASSNAME], spltInfo[CONFIG_PATH], logger, producer);
                    else
                        err = CreateExecutor(spltInfo[CLASSNAME], spltInfo[CONFIG_PATH], logger, reader);
                    if (err != ErrorsHandle.NO_ERRORS) {
                        logger.log(Level.INFO, err.GetDescription());
                        return err;
                    }
                    if (producer == null) {
                        reader.addConsumer(exList.get(exList.size() - 1));
                        exList.get(exList.size() - 1).addProducer(reader);
                    }
                    else {
                        producer.addConsumer(exList.get(exList.size() - 1));
                    }
                    paramSet = true;
                    producer = exList.get(exList.size() - 1);
                    break;
                }
            }
            if (!paramSet) {
                ErrorsHandle err = ErrorsHandle.CONFIG_ARGS;
                err.AddInformation("No description of executor: " + exName);
                logger.log(Level.INFO, err.GetDescription());
                return err;
            }
        }
        if (producer != null)
        {
            producer.addConsumer(writer);
            writer.addProducer(producer);
        }
        else
        {
            reader.addConsumer(writer);
            writer.addProducer(reader);
        }
        return ErrorsHandle.NO_ERRORS;
    };


    public void runConvey() {
        reader.run();
        if (reader.status() == Status.OK) {
            logger.log(Level.INFO, "Everything worked fine.");
        }
        else {
            logger.log(Level.INFO, "Error occurred during running the convey.");
        }
    }



    private ErrorsHandle CreateExecutor (String className, String config, Logger logger, Producer producer)
    {
        Executor execInst;
        try {
            Class c = Class.forName(className);
            Class[] params = {String.class, ru.spbstu.pipeline.logging.Logger.class};
            execInst = (Executor) c.getConstructor(params).newInstance(config, UtilLogger.of(logger));
        }
        catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |InstantiationException | InvocationTargetException ex) {
            logger.log(Level.INFO, "Executor creation exception: ", ex);
            return ErrorsHandle.REFL_EXEPTION;
        }
        execInst.addProducer(producer);
        exList.add(execInst);

        if (execInst.status() != Status.OK)
            return ErrorsHandle.EX_ERR;
        return ErrorsHandle.NO_ERRORS;
    }


    public static void main(String[] args) {
        Manager manager = new Manager();
        String logPath = "log.txt";
        if (manager.InitLogging(logPath) != ErrorsHandle.NO_ERRORS) {
            System.exit(-1);
        }
        if (args.length < 1) {
            manager.Log("Not enough args: must be at least one");
            System.exit(1);
        }
        if (manager.ReadConfig(args[0]) != ErrorsHandle.NO_ERRORS) {
            System.exit(2);
        }
        manager.runConvey();
    }

}
