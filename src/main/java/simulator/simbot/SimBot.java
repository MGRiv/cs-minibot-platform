package simulator.simbot;

import basestation.bot.robot.Bot;
import basestation.bot.sensors.SensorCenter;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import simulator.physics.PhysicalObject;

import java.io.*;
import java.net.*;
import java.util.Map;

public class SimBot extends Bot {

    private final transient SimBotCommandCenter commandCenter;
    private final transient SimBotSensorCenter sensorCenter;
    public transient TCPServer runningThread;
    public transient DataLog loggingThread;
    private transient PhysicalObject myPhysicalObject;


    /**
     * Currently minibots are implemented using a TCP connection
     */

    public SimBot(SimBotConnection sbc, String name, PhysicalObject myPhysicalObject) {
        super(sbc, name);
        this.commandCenter = new SimBotCommandCenter(this);
        this.sensorCenter = new SimBotSensorCenter();
        this.myPhysicalObject = myPhysicalObject;

        try {
            Thread t = new TCPServer(11111, this, this.commandCenter, this.sensorCenter);
            this.runningThread = (TCPServer)t;
            this.runningThread.start();

            Thread log = new DataLog("log.csv", this.commandCenter, this.sensorCenter);
            this.loggingThread = (DataLog)log;
            this.loggingThread.start();
        }catch(IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public SimBotCommandCenter getCommandCenter() {
        return commandCenter;
    }

    @Override
    public SensorCenter getSensorCenter() {
        return sensorCenter;
    }

    public void resetServer() { this.runningThread.stopStream(); }

    public class DataLog extends Thread {
        private final transient SimBotCommandCenter commandCenter;
        private final transient SimBotSensorCenter sensorCenter;
        private static final char DEFAULT_SEPARATOR = ',';
        private final String path;
        private FileWriter writer;
        private boolean fileCreated = false;
        private volatile boolean exit = false;
        private static final int INTERVAL = 250;

        public DataLog(String path, SimBotCommandCenter commandCenter, SimBotSensorCenter sensorCenter) throws IOException{
            this.commandCenter = commandCenter;
            this.sensorCenter = sensorCenter;
            this.path = path;
            if (commandCenter.isLogging()) {
                File file = new File(path);
                file.createNewFile();
                this.writer = new FileWriter(file);
                this.fileCreated = true;
            }
        }

        public void stopLogging() {
            this.exit = true;
        }

        public void myRun() throws Exception {
            boolean first_header = true;
            while (!exit) {
                if (this.commandCenter.isLogging()) {
                    //Creates csv file if it hasn't been created yet
                    if (!fileCreated) {
                        File file = new File(this.path);
                        file.createNewFile();
                        this.writer = new FileWriter(file);
                        this.fileCreated = true;
                    }

                    //Adjusts the interval of sampling data
                    this.sleep(INTERVAL);

                    //Grabs command data
                    boolean first = true;
                    StringBuilder sb_command = new StringBuilder();
                    StringBuilder sb_header_command = new StringBuilder();
                    JsonObject commandData = this.commandCenter.getAllData();
                    for (Map.Entry<String, JsonElement> entry : commandData.entrySet()) {
                        String name = entry.getKey();
                        String value = entry.getValue().getAsString();
                        if (!first) {
                            sb_command.append(DEFAULT_SEPARATOR);
                            sb_header_command.append(DEFAULT_SEPARATOR);
                        }
                        sb_command.append(value);
                        sb_header_command.append(name);

                        first = false;
                    }

                    //Grabs sensor data
                    StringBuilder sb_sensor = new StringBuilder();
                    StringBuilder sb_header_sensor = new StringBuilder();
                    JsonObject sensorData = this.sensorCenter.getAllDataGson();
                    for (Map.Entry<String, JsonElement> entry : sensorData.entrySet()) {
                        String name = entry.getKey();
                        JsonObject data = entry.getValue().getAsJsonObject();
                        String value = Integer.toString(data.get("data").getAsInt());
                        sb_sensor.append(DEFAULT_SEPARATOR);
                        sb_header_sensor.append(DEFAULT_SEPARATOR);
                        if (value.equals("")) {
                            value = " ";
                        }
                        sb_sensor.append(value);
                        sb_header_sensor.append(name);
                    }


                    //Record Header if there is a header
                    if (first_header &&
                            !sb_header_sensor.toString().equals("") &&
                            !sb_header_command.toString().equals("")) {
                        sb_header_command.append(sb_header_sensor);
                        sb_header_command.append("\n");
                        writer.append(sb_header_command.toString());
                        first_header = false;
                    }

                    //Write to csv
                    if (!sb_command.toString().equals("") && !sb_sensor.toString().equals("")) {
                        sb_command.append(sb_sensor);
                        sb_command.append("\n");
                        writer.append(sb_command.toString());
                        writer.flush();
                    }
                }
            }
            writer.close();
        }

        public void run(){
            try {
                this.myRun();
            } catch(Exception e) {
                System.out.println("ERROR IN LOGGING");
            }
        }


    }
    public class TCPServer extends Thread {
        private ServerSocket serverSocket;
        private final transient SimBot sim;
        private final transient SimBotCommandCenter commandCenter;
        private final transient SimBotSensorCenter sensorCenter;
        private volatile boolean exit = false;

        public TCPServer(int port, SimBot sim, SimBotCommandCenter commandCenter, SimBotSensorCenter sensorCenter) throws IOException {
            this.sim = sim;
            this.commandCenter = commandCenter;
            this.sensorCenter = sensorCenter;
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(100000);
        }

        public void stopStream() {
            exit = true;
        }

        public void run() {
            while (true) {
                try {
                    boolean run = true;
                    System.out.println("Waiting for client on port " +
                            serverSocket.getLocalPort() + "...");
                    Socket server = serverSocket.accept();

                    System.out.println("Just connected to " + server.getRemoteSocketAddress());

                    String content;
                    BufferedReader in = new BufferedReader(new InputStreamReader(server.getInputStream()));
                    PrintWriter out = new PrintWriter(server.getOutputStream(), true);
                    exit = false;

                    while (run && !exit) {
                        content = in.readLine();

                        if (content != null) {
//                            double value = 0;
                            String value = "";
                            if (!content.contains("WHEELS") && !content.contains("REGISTER") && !content.contains("GET")) {
//                                value = Double.parseDouble(content.substring(content.indexOf(':') + 1));
                                value = content.substring(content.indexOf(':') + 1);
                            }
                            switch (content.substring(0, content.indexOf(':'))) {
                                case "FORWARD":
                                    //fl fr bl br
//                                    this.commandCenter.setWheelPower(value, value, value, value);
                                    this.commandCenter.sendKV("WHEELS",
                                            "<<<<WHEELS," + value + "," + value + "," + value + "," + value + ">>>>");
                                    System.out.println("FORWARD " + value);
                                    break;
                                case "BACKWARD":
//                                    this.commandCenter.setWheelPower(-value, -value, -value, -value);
                                    this.commandCenter.sendKV("WHEELS",
                                            "<<<<WHEELS,-" + value + ",-" + value + ",-" + value + ",-" + value + ">>>>");
                                    System.out.println("BACKWARD " + value);
                                    break;
                                case "RIGHT":
//                                    this.commandCenter.setWheelPower(value, -value, value, -value);
                                    this.commandCenter.sendKV("WHEELS",
                                            "<<<<WHEELS," + value + ",-" + value + "," + value + ",-" + value + ">>>>");
                                    System.out.println("RIGHT " + value);
                                    break;
                                case "LEFT":
//                                    this.commandCenter.setWheelPower(-value, value, -value, value);
                                    this.commandCenter.sendKV("WHEELS",
                                            "<<<<WHEELS,-" + value + "," + value + ",-" + value + "," + value + ">>>>");
                                    System.out.println("LEFT " + value);
                                    break;
                                case "WAIT":
                                    System.out.println("WAITING FOR " + value + " SECONDS");
                                    break;
                                case "STOP":
//                                    this.commandCenter.setWheelPower(0, 0, 0, 0);
                                    this.commandCenter.sendKV("WHEELS", "<<<<WHEELS,0,0,0,0>>>>");
                                    System.out.println("STOPPING");
                                    break;
                                case "KILL":
                                    run = false;
                                    System.out.println("Exiting\n");
                                    break;
                                case "GET":
                                    String name = content.substring(content.indexOf(':') + 1);
                                    if (name.equals("ALL")) {
                                        out.println(this.sensorCenter.getAllDataGson());
                                    } else {
                                        out.println(this.sensorCenter.getSensorData(name));
                                    }
                                    System.out.println("Returning " + name + " data");
                                    break;
                                case "REGISTER":
//                                    String name = content.substring(content.indexOf(':') + 1);
//                                    ColorIntensitySensor colorSensor = new ColorIntensitySensor((SimBotSensorCenter) this.sensorCenter,name, this.sim);
                                    System.out.println("Added New sensor");
                                    break;
                                default:
                                    String cmd = content.substring(content.indexOf(':') + 1);
                                    System.out.println(cmd);
                                    String[] wheel_cmds = cmd.split(",");


//                                    this.commandCenter.setWheelPower(
//                                            Double.parseDouble(wheel_cmds[0]),
//                                            Double.parseDouble(wheel_cmds[1]),
//                                            Double.parseDouble(wheel_cmds[2]),
//                                            Double.parseDouble(wheel_cmds[3])
//                                    );

                                    this.commandCenter.sendKV("WHEELS", "<<<<WHEELS," + wheel_cmds[0] + "," + wheel_cmds[1]
                                            + "," + wheel_cmds[2] + "," + wheel_cmds[3] + ">>>>");
                                    break;

                            }
                        }
                    }
                } catch (SocketTimeoutException s) {
                    System.out.println("Socket timed out!");
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }

            }
        }
    }
}

