package simulator.simbot;

import basestation.bot.commands.FourWheelMovement;
import basestation.bot.robot.Bot;
import basestation.bot.sensors.SensorCenter;
import simulator.physics.PhysicalObject;

import java.io.*;
import java.net.*;

public class SimBot extends Bot {

    private final transient SimBotCommandCenter commandCenter;
    private final transient SimBotSensorCenter sensorCenter;
    public transient TCPServer runningThread;
    private transient PhysicalObject myPhysicalObject;


    /**
     * Currently minibots are implemented using a TCP connection
     */

    public SimBot(SimBotConnection sbc, String name, PhysicalObject myPhysicalObject) {
        super(sbc);
        this.commandCenter = new SimBotCommandCenter(this);
        this.sensorCenter = new SimBotSensorCenter();
        this.myPhysicalObject = myPhysicalObject;

        try {
            Thread t = new TCPServer(11111, this.commandCenter, this.sensorCenter);
            this.runningThread = (TCPServer)t;
            this.runningThread.start();
//            t.start();
        }catch(IOException e) {
            e.printStackTrace();
        }

//        Reader r = new Reader(this);
//        r.start();
    }

    @Override
    public SimBotCommandCenter getCommandCenter() {
        return commandCenter;
    }

    @Override
    public SensorCenter getSensorCenter() {
        return sensorCenter;
    }

    private class Reader extends Thread {
        private SimBot parent;

        public Reader(SimBot parent) {
            this.parent = parent;
        }

        public void run() {
            while (true) {
                System.out.println(parent.sensorCenter.getAllDataGson());
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void resetServer() { this.runningThread.stopStream(); }

    public class TCPServer extends Thread {
        private ServerSocket serverSocket;
        private final transient SimBotCommandCenter commandCenter;
        private final transient SimBotSensorCenter sensorCenter;
        private volatile boolean exit = false;

        public TCPServer(int port, SimBotCommandCenter commandCenter, SimBotSensorCenter sensorCenter) throws IOException {
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
                            double value = 0;
                            if (!content.contains("WHEELS")) {
                                value = Double.parseDouble(content.substring(content.indexOf(':') + 1));
                            }
                            switch (content.substring(0, content.indexOf(':'))) {
                                case "FORWARD":
                                    //fl fr bl br
                                    this.commandCenter.setWheelPower(value, value, value, value);
                                    System.out.println("FORWARD " + value);
                                    break;
                                case "BACKWARD":
                                    this.commandCenter.setWheelPower(-value, -value, -value, -value);
                                    System.out.println("BACKWARD " + value);
                                    break;
                                case "RIGHT":
                                    this.commandCenter.setWheelPower(value, -value, value, -value);
                                    System.out.println("RIGHT " + value);
                                    break;
                                case "LEFT":
                                    this.commandCenter.setWheelPower(-value, value, -value, value);
                                    System.out.println("LEFT " + value);
                                    break;
                                case "WAIT":
                                    System.out.println("WAITING FOR " + value + " SECONDS");
                                    break;
                                case "STOP":
                                    this.commandCenter.setWheelPower(0, 0, 0, 0);
                                    System.out.println("STOPPING");
                                    break;
                                case "KILL":
                                    run = false;
                                    System.out.println("Exiting\n");
                                    break;
                                case "GET":
//                                    out.println("How much data can it send back");
                                    out.println(this.sensorCenter.getAllDataGson());
                                    System.out.println("Returning Sensor Data");
                                    break;
                                default:
                                    String cmd = content.substring(content.indexOf(':') + 1);
                                    System.out.println(cmd);
                                    String[] wheel_cmds = cmd.split(",");


                                    this.commandCenter.setWheelPower(
                                            Double.parseDouble(wheel_cmds[0]),
                                            Double.parseDouble(wheel_cmds[1]),
                                            Double.parseDouble(wheel_cmds[2]),
                                            Double.parseDouble(wheel_cmds[3])
                                    );
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

