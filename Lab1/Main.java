package com.company;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;


public class Main {


    private static ErrorsHandle CompressFile (ConfigParser configs, BufferedInputStream inStream, BufferedOutputStream outStream) {
        byte[] input = new byte[configs.getBufferLimit()];
        int bytesRead = 0;
        try {
            bytesRead = inStream.read(input);
        }
        catch (IOException ex) {
            ErrorsHandle err = ErrorsHandle.INPUT_READ;
            err.AddInformation(ex.getMessage());
            return err;
        }
        byte[] result;
        while (bytesRead != -1) {
            result = Algorithm.RLE(Arrays.copyOfRange(input, 0, bytesRead));
            try {
                outStream.write(result);
                outStream.flush();
            }
            catch (IOException ex) {
                ErrorsHandle err = ErrorsHandle.OUTPUT_WRITE;
                err.AddInformation(ex.getMessage());
                return err;
            }
            try {
                bytesRead = inStream.read(input);
            }
            catch (IOException ex) {
                ErrorsHandle err = ErrorsHandle.INPUT_READ;
                err.AddInformation(ex.getMessage());
                return err;
            }
        }
        return ErrorsHandle.NO_ERRORS;
    }


    private static ErrorsHandle DecompressFile (ConfigParser configs, BufferedInputStream inStream, BufferedOutputStream outStream)  {
        byte[] input = new byte[configs.getBufferLimit()];
        int bytesRead = 0;
        try {
            bytesRead = inStream.read(input);
        }
        catch (IOException ex) {
            ErrorsHandle err = ErrorsHandle.INPUT_READ;
            err.AddInformation(ex.getMessage());
            return err;
        }
        byte[] result;
        while (bytesRead != -1) {
            result = Algorithm.RLE_Decode(Arrays.copyOfRange(input, 0, bytesRead));
            try {
                outStream.write(result);
                outStream.flush();
            }
            catch (IOException ex) {
                ErrorsHandle err = ErrorsHandle.OUTPUT_WRITE;
                err.AddInformation(ex.getMessage());
                return err;
            }
            try {
                bytesRead = inStream.read(input);
            }
            catch (IOException ex) {
                ErrorsHandle err = ErrorsHandle.INPUT_READ;
                err.AddInformation(ex.getMessage());
                return err;
            }
        }
        return ErrorsHandle.NO_ERRORS;
    }

    public static ErrorsHandle ToBack(ConfigParser configs, BufferedInputStream inStream, BufferedOutputStream outStream) {
        byte[] input = new byte[configs.getBufferLimit()];
        int bytesRead = 0;
        try {
            bytesRead = inStream.read(input);
        }
        catch (IOException ex) {
            ErrorsHandle err = ErrorsHandle.INPUT_READ;
            err.AddInformation(ex.getMessage());
            return err;
        }
        while (bytesRead != -1) {
            try {
                outStream.write(Arrays.copyOfRange(input, 0, bytesRead));
                outStream.flush();
            }
            catch (IOException ex) {
                ErrorsHandle err = ErrorsHandle.OUTPUT_WRITE;
                err.AddInformation(ex.getMessage());
                return err;
            }
            try {
                bytesRead = inStream.read(input);
            }
            catch (IOException ex) {
                ErrorsHandle err = ErrorsHandle.INPUT_READ;
                err.AddInformation(ex.getMessage());
                return err;
            }
        }
        return ErrorsHandle.NO_ERRORS;
    }

    public static void main(String[] args) {
        try {
            //Open log-file
            FileWriter toLog = new FileWriter("log.txt");
            //Check number of arguments
            if (args.length < 1) {
                toLog.write("Wrong number of program arguments: " + args.length + ". Required one.");
                toLog.close();
                System.exit(1);
            }
            //Get configurations
            ConfigParser configs = new ConfigParser();
            ErrorsHandle err = configs.parse(args[0]);
            if (err != ErrorsHandle.NO_ERRORS) {
                toLog.write(err.GetDescription());
                toLog.close();
                System.exit(1);
            }
            try {
                FileInputStream in = new FileInputStream(configs.getInFilename());
                FileOutputStream out = new FileOutputStream(configs.getOutFilename());
                BufferedInputStream inStream = new BufferedInputStream(in, configs.getBufferLimit());
                BufferedOutputStream outStream = new BufferedOutputStream(out);
                /*if (configs.getMode()) {
                    err = DecompressFile(configs, inStream, outStream);
                }
                else {
                    err = CompressFile(configs, inStream, outStream);
                }*/
                err = ToBack(configs, inStream, outStream);
                inStream.close();
                outStream.close();
                in.close();
                out.close();
                if (err != ErrorsHandle.NO_ERRORS) {
                    toLog.write(err.GetDescription());
                    toLog.close();
                    System.exit(1);
                }
            }
            catch (IOException ex) {
                toLog.write("Not possible to open file: " + ex.getMessage());
                toLog.close();
                System.exit(1);
            }
            toLog.close();
        }
        catch (IOException ex) {
            System.out.println("Not possible to open log file. " + ex.getMessage());
            System.exit(1);
        }
    }
}
