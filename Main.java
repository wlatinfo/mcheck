package eu.izadpanah.mcheck;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.deploy.util.BlackList;
import org.json.JSONException;
import org.json.JSONObject;

public class Main {
    protected static final Logger LOGGER = LoggerConfig.getLogger();

    // Thread-safe flags
    public static AtomicBoolean sentRmoteAgeAccept = new AtomicBoolean(false);
    public static AtomicBoolean countDownfinished = new AtomicBoolean(false);
    public static AtomicBoolean startCountDown= new AtomicBoolean(false);
    public static boolean rejectShow;

    protected static String osType;
    protected static ITabSocketServer itabsocketserver;
    protected static boolean itabOnline=false;
    protected static int ageThreashold=25;

    protected static ITabHandler iTabHandler;
    protected static FlowControl flowControl;
    protected static AuthFrame authFrame;
    protected static MyCheckerHandler myCheckerHandler;

    protected static ICUService icuService;
    protected static String api;
    protected static String streamurl;
    protected static String username;
    protected static String password;
    protected static String token;
    protected static int waiting;
    protected static boolean icuLog;

    protected static String tranState="close";
    protected static String transSession="End session";

    public static AtomicBoolean transVerify = new AtomicBoolean(false);
    public static AtomicBoolean transHelp = new AtomicBoolean(false);
    protected static String mID ="";

    public static AtomicBoolean verificationCall = new AtomicBoolean(false);
    public static AtomicBoolean verificationResult = new AtomicBoolean(false);
    public static AtomicBoolean approvePrivacy = new AtomicBoolean(false);
    public static AtomicBoolean verificationTimeout = new AtomicBoolean(false);

    public static AtomicBoolean checkCase = new AtomicBoolean(false);

    protected static Path ConfigFilePath;
    protected static JSONObject app;
    protected static JSONObject server;
    protected static JSONObject icu;
    protected static int socketPort;
    protected static boolean iTabLog;
    protected static String procesState="none";

    protected static String title;
    protected static String font;

    public static void main(String[] args) {
        try {
            LoggerConfig.setup();
            logFilesHandel();
            initialConfig();

            itabsocketserver  = new ITabSocketServer(socketPort,5);
            new Thread(itabsocketserver).start();

            icuService = new ICUService();
            new Thread(icuService).start();

            iTabHandler = new ITabHandler();
            new Thread(iTabHandler).start();

            itabsocketserver.addInboundListener((client, xmlPayload) -> {
                if (iTabLog) LoggerConfig.log("iTab Log : ", Level.INFO, xmlPayload);
            });

            icuService.handleToken();

            flowControl = new FlowControl();
            new Thread(flowControl).start();

            authFrame = new AuthFrame();

            myCheckerHandler = new MyCheckerHandler();
            new Thread(myCheckerHandler).start();

            myCheckerHandler.disableFaceprocess();
            JSONObject jstatus = myCheckerHandler.getStatus();
            LoggerConfig.log("Main.MyCheckerHandler.getStatus", Level.INFO, jstatus != null ? jstatus.toString() : "null");

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static void logFilesHandel() {
        String os = System.getProperty("os.name");
        LoggerConfig.log("Main.LogFileHandel",Level.INFO,"Operation System is: "+os);
        if(os.startsWith("Windows")){
            osType="windows";
//            searchCreteria = "Get-Content -wait std_pos.log | Select-String -Pattern 'eventId:','"; //only Development - on windows
            //searchCreteria = "Get-Content -Tail 50 -wait c:\\gkretail\\pos\\log\\std_pos.log | Select-String -Pattern 'eventId:','";  // for Production
            //commander="powershell.exe";
        } else if (os.startsWith("Linux")) {
            osType="linux";
//            searchCreteria="tail -f /usr/local/gkretail/pos/log/std_pos.log | egrep --line-buffered -B1 -A3 -i -e 'eventId:' -e '";
            //searchCreteria="tail -f /usr/local/gkretail/pos/log/std_pos.log | egrep --line-buffered -B1 -A3 -i -e 'eventId:'";
            //commander="/bin/bash";
        }
        LocalDate currentDate = LocalDate.now();
        Path logFilePath= Paths.get("log");
        LocalDate thirtyDaysAgo = currentDate.minus(Period.ofDays(30));
        for (File file: Objects.requireNonNull(logFilePath.toFile().listFiles())){
            if(file.isFile()){
                String logfile =file.getName();
                Path filePath = Paths.get(file.getAbsolutePath());
                if(logfile.endsWith(".lck")){
                    try {
                        Files.delete(filePath);
                        System.out.println("Lck File has been deleted! "+logfile);
                    }catch (Exception ex){
                        System.out.println("lck File won't be deleted! "+ logfile);
                    }
                }else {
                    if(logfile.startsWith("myChecker_")){
                        String dateOfFile = logfile.substring(10,18);
                        LocalDate fileDate = LocalDate.parse(dateOfFile, DateTimeFormatter.ofPattern("yyyyMMdd"));
                        if(fileDate.isBefore(thirtyDaysAgo)){
                            try {
                                Files.delete(filePath);
                                System.out.println("Log File has been deleted! "+logfile);
                            }catch (Exception ex){
                                System.out.println("lcg File won't be deleted! "+ logfile);
                            }
                        }
                    }
                }
            }
        }
    }
    protected static void initialConfig() {
        ConfigFilePath = Paths.get("config/application.json");
        String jsonContent = null;
        try {
            jsonContent = new String(Files.readAllBytes(ConfigFilePath));
        } catch (IOException e) {
            LoggerConfig.log("Main.InitialConfig",Level.WARNING,"The Log File is not located properly!");
        }

        JSONObject Configs = null;
        try {
            Configs = new JSONObject(jsonContent);

            app = Configs.getJSONObject("app");
            server = Configs.getJSONObject("server");
            socketPort = server.getInt("port");
            iTabLog=server.getBoolean("log");
            icu = Configs.getJSONObject("icu");

            title=app.getString("title");
            font = app.getString("font");

            api = icu.getString("api");
            streamurl=icu.getString("stream");
            username = icu.getString("username");
            password=icu.getString("password");
            waiting = icu.getInt("check_wait");
            icuLog=icu.getBoolean("log");

        } catch (JSONException e) {
            System.out.println("Error in Config Initialization"+e);
        }
    }

}