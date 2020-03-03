package myExecutor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.Arrays;


public class Processing {


    public static ExecErrors CompressFile (ConfigParser configs, BufferedInputStream inStream, BufferedOutputStream outStream) {
        byte[] input = new byte[configs.getBufferLimit()];
        int bytesRead = 0;
        try {
            bytesRead = inStream.read(input);
        }
        catch (IOException ex) {
            ExecErrors err = ExecErrors.INPUT_READ;
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
                ExecErrors err = ExecErrors.OUTPUT_WRITE;
                err.AddInformation(ex.getMessage());
                return err;
            }
            try {
                bytesRead = inStream.read(input);
            }
            catch (IOException ex) {
                ExecErrors err = ExecErrors.INPUT_READ;
                err.AddInformation(ex.getMessage());
                return err;
            }
        }
        return ExecErrors.NO_ERRORS;
    }


    public static ExecErrors DecompressFile (ConfigParser configs, BufferedInputStream inStream, BufferedOutputStream outStream)  {
        byte[] input = new byte[configs.getBufferLimit()];
        int bytesRead = 0;
        try {
            bytesRead = inStream.read(input);
        }
        catch (IOException ex) {
            ExecErrors err = ExecErrors.INPUT_READ;
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
                ExecErrors err = ExecErrors.OUTPUT_WRITE;
                err.AddInformation(ex.getMessage());
                return err;
            }
            try {
                bytesRead = inStream.read(input);
            }
            catch (IOException ex) {
                ExecErrors err = ExecErrors.INPUT_READ;
                err.AddInformation(ex.getMessage());
                return err;
            }
        }
        return ExecErrors.NO_ERRORS;
    }

    public static ExecErrors ToBack(ConfigParser configs, BufferedInputStream inStream, BufferedOutputStream outStream) {
        byte[] input = new byte[configs.getBufferLimit()];
        int bytesRead = 0;
        try {
            bytesRead = inStream.read(input);
        }
        catch (IOException ex) {
            ExecErrors err = ExecErrors.INPUT_READ;
            err.AddInformation(ex.getMessage());
            return err;
        }
        while (bytesRead != -1) {
            try {
                outStream.write(Arrays.copyOfRange(input, 0, bytesRead));
                outStream.flush();
            }
            catch (IOException ex) {
                ExecErrors err = ExecErrors.OUTPUT_WRITE;
                err.AddInformation(ex.getMessage());
                return err;
            }
            try {
                bytesRead = inStream.read(input);
            }
            catch (IOException ex) {
                ExecErrors err = ExecErrors.INPUT_READ;
                err.AddInformation(ex.getMessage());
                return err;
            }
        }
        return ExecErrors.NO_ERRORS;
    }
}
