package com.company;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class ConfigParser {

    private static final String DELIMITER = "=";

    public enum Grammar {
        FILE_IN ("FILE IN"),
        FILE_OUT ("FILE OUT"),
        BUFFER_LIMIT("LIMIT"),
        DECODE ("DECODE");


        private String title;

        Grammar(String title) {
            this.title = title;
        }

        public String getTitle(){ return title;}
    }


    private String inputFilename;
    private String outputFilename;
    private int bufferLimit;
    private boolean decodeMode;

    public ErrorsHandle parse (String configFilename) {
        decodeMode = false;
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
                        case DECODE:
                            decodeMode = true;
                    }
                    paramSet = true;
                    break;
                }
            }
            if (!paramSet && param != Grammar.DECODE) {
                ErrorsHandle err = ErrorsHandle.CONFIG_ARGS;
                err.AddInformation(param.getTitle());
                return err;
            }
        }
        return ErrorsHandle.NO_ERRORS;
    };

    public String getInFilename() {
        return inputFilename;
    }

    public String getOutFilename() {
        return outputFilename;
    }

    public boolean getMode() {
        return decodeMode;
    }

    public int getBufferLimit() {
        return bufferLimit;
    }
}