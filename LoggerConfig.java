package eu.izadpanah.mcheck;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class LoggerConfig {
    private static final Logger LOGGER = Logger.getLogger("SL_");
    private static final String LOG_DIR = "log";
    private static final String LOG_FILE_PATTERN = "yyyyMMdd";
    protected static final int LOG_FILE_SIZE = 10 * 1024 * 1024; // 10 MB per log file
    protected static final ExecutorService logExecutor = Executors.newSingleThreadExecutor();
    static String currentLogFile;
    static String dateFormatted;

    public static void setup() throws IOException {
        // Ensure the log directory exists
        Path logDirPath = Paths.get(LOG_DIR);
        if (!Files.exists(logDirPath)) {
            Files.createDirectories(logDirPath);
        }

        // Set up the file handler with the full path
        String logFilePath = getLogFileName();
        //FileHandler fileHandler = new FileHandler(logFilePath, LOG_FILE_SIZE, 1, true); // if multiple log files needed
        FileHandler fileHandler = new FileHandler(logFilePath,true);
        fileHandler.setFormatter(new CustomFormatter());
        LOGGER.addHandler(fileHandler);
    }

    private static String getLogFileName() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(LOG_FILE_PATTERN);
        dateFormatted = dateFormat.format(new Date());
        currentLogFile =Paths.get(LOG_DIR, "myChecker_" + dateFormatted + ".log").toString();
        return currentLogFile;
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public static void log(String cf,Level level, String message) {
        try {
            logExecutor.submit(() -> LOGGER.log(level, message,cf));
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void shutdown() {
        try {
            //logExecutor.shutdown();
            if (logExecutor.awaitTermination(20, TimeUnit.SECONDS)) {
                logExecutor.shutdown();
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    private static class CustomFormatter extends Formatter {
        private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss:SSS");

        @Override
        public String format(LogRecord record) {
            StringBuilder sb = new StringBuilder();
            sb.append(dateFormat.format(new Date(record.getMillis())))
                    .append(" ")
                    .append(record.getLevel().getName())
                    .append(": ")
                    .append(Arrays.stream(record.getParameters()).findFirst().get())
                    .append(" - ")
                    .append(formatMessage(record))
                    .append(System.lineSeparator());
            return sb.toString();
        }
    }
}


