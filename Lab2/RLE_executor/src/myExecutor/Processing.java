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
        byte[] newInput = new byte[configs.getBufferLimit()];
        int bytesRead = 0;
        try {
            bytesRead = inStream.read(newInput);
        }
        catch (IOException ex) {
            ExecErrors err = ExecErrors.INPUT_READ;
            err.AddInformation(ex.getMessage());
            return err;
        }
        byte[] result;
        byte[] leftover;
        int decodeSize = 0;
        while (bytesRead != -1) {
            decodeSize += bytesRead;
            do {
                decodeSize--;
            } while (decodeSize >= 0 && newInput[decodeSize] > 0);
            if (decodeSize > 0) {
                result = Algorithm.RLE_Decode(Arrays.copyOfRange(newInput, 0, decodeSize));
                try {
                    outStream.write(result);
                    outStream.flush();
                }
                catch (IOException ex) {
                    ExecErrors err = ExecErrors.OUTPUT_WRITE;
                    err.AddInformation(ex.getMessage());
                    return err;
                }
                leftover = Arrays.copyOfRange(newInput, decodeSize, newInput.length);
                try {
                    bytesRead = inStream.read(input);
                }
                catch (IOException ex) {
                    ExecErrors err = ExecErrors.INPUT_READ;
                    err.AddInformation(ex.getMessage());
                    return err;
                }
                if (bytesRead == -1) {
                    result = Algorithm.RLE_Decode(leftover);
                    try {
                        outStream.write(result);
                        outStream.flush();
                    }
                    catch (IOException ex) {
                        ExecErrors err = ExecErrors.OUTPUT_WRITE;
                        err.AddInformation(ex.getMessage());
                        return err;
                    }
                } else {
                    newInput = Arrays.copyOf(leftover, leftover.length + input.length);
                    System.arraycopy(input, 0, newInput, leftover.length, input.length);
                    decodeSize = leftover.length;
                }

            } else if (decodeSize == 0) {
                result = Algorithm.RLE_Decode(Arrays.copyOfRange(newInput, 0, bytesRead));
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
                    bytesRead = inStream.read(newInput);
                }
                catch (IOException ex) {
                    ExecErrors err = ExecErrors.INPUT_READ;
                    err.AddInformation(ex.getMessage());
                    return err;
                }
            } else {
                try {
                    bytesRead = inStream.read(input);
                }
                catch (IOException ex) {
                    ExecErrors err = ExecErrors.INPUT_READ;
                    err.AddInformation(ex.getMessage());
                    return err;
                }
                decodeSize = newInput.length;
                newInput = Arrays.copyOf(newInput, newInput.length + input.length);
                System.arraycopy(input, 0, newInput, newInput.length, input.length);
            }
        }
        return ExecErrors.NO_ERRORS;
    }
}
