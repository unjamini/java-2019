package myExecutor;


public enum ExecErrors {
    NO_ERRORS (0, "Fine."),
    CONFIG_READ(1, "Problems with reading config-file."),
    CONFIG_ARGS(2, "Not enough arguments in config-file (or some of them are wrong): "),
    INPUT_READ(3, "Not possible to read input file."),
    OUTPUT_WRITE(4, "Not possible to write to output file.");


    private final int code;
    private final String description;
    private String addInfo;
    boolean addInfoSet = false;

    ExecErrors(int i, String s) {
        this.code = i;
        this.description = s;
    }

    void AddInformation (String s) {
        this.addInfo = s;
        this.addInfoSet = true;
    }

    String GetDescription () {
        if (addInfoSet) {
            return this.description + this.addInfo;
        }
        else {
            return this.description;
        }
    }
}

