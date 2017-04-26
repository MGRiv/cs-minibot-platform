package minibot;

import basestation.BaseStation;
import basestation.bot.commands.FourWheelMovement;
import basestation.bot.connection.IceConnection;
import basestation.bot.connection.TCPConnection;
import basestation.bot.robot.Bot;
import basestation.bot.robot.minibot.MiniBot;
import basestation.bot.robot.modbot.ModBot;
import basestation.vision.OverheadVisionSystem;
import basestation.vision.VisionCoordinate;
import basestation.vision.VisionObject;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import examples.gobot.Course;
import examples.gobot.GoBot;
import examples.patrol.Patrol;
import simulator.physics.PhysicalObject;
import simulator.simbot.ColorIntensitySensor;
import simulator.simbot.SimBotConnection;
import simulator.simbot.SimBotSensorCenter;
import spark.route.RouteOverview;

import java.util.*;


import simulator.baseinterface.SimulatorVisionSystem;


import simulator.simbot.SimBot;
import xboxhandler.XboxControllerDriver;

import static spark.Spark.*;

/**
 * HTTP connections for modbot GUI
 * <p>
 * Used for modbot to control and use the GUI while connecting to basestation.
 * */
public class BaseHTTPInterface {

    // Temp config settings
    public static final boolean OVERHEAD_VISION = true;
    private static XboxControllerDriver xboxControllerDriver;
    public static ArrayList<VisionCoordinate> patrolPoints;


    public static void main(String[] args) {
        // Spark configuration
        port(8080);
        staticFiles.location("/public");
        RouteOverview.enableRouteOverview("/");
        SimulatorVisionSystem simvs;
        // Show exceptions
        exception(Exception.class, (exception,request,response) -> {
            exception.printStackTrace();
            response.status(500);
            response.body("oops");
        });

        // Global objects
        JsonParser jp = new JsonParser();
        Gson gson = new Gson();
        patrolPoints = new ArrayList<>();
        Course course = new Course();
        ArrayList<VisionCoordinate> outerTrackCoords = new ArrayList<>();
        ArrayList<VisionCoordinate> innerTrackCoords = new ArrayList<>();
        ArrayList<VisionCoordinate> startAreaCoords = new ArrayList<>();
        ArrayList<VisionCoordinate> middleAreaCoords = new ArrayList<>();
        //HARD-CODED COORDINATES
        innerTrackCoords.addAll(Arrays.asList(new VisionCoordinate(2,2), new
                VisionCoordinate(3,2), new VisionCoordinate(3,3), new
                VisionCoordinate(2,3)));
        outerTrackCoords.addAll(Arrays.asList(new VisionCoordinate(1,1), new
                VisionCoordinate(4,1), new VisionCoordinate(4,4), new
                VisionCoordinate(1,4)));
        startAreaCoords.addAll(Arrays.asList(new VisionCoordinate(3,2), new
                VisionCoordinate(4,2), new VisionCoordinate(4,2.25), new
                VisionCoordinate(3,2.25)));
        middleAreaCoords.addAll(Arrays.asList(new VisionCoordinate(2,3), new
                VisionCoordinate(2,2.75), new VisionCoordinate(1,2.75), new
                VisionCoordinate(1,3)));
        GoBot gb = new GoBot();
        Long timer = 0L;

        if (OVERHEAD_VISION) {
            OverheadVisionSystem ovs = new OverheadVisionSystem();
            BaseStation.getInstance().getVisionManager().addVisionSystem(ovs);
            simvs = SimulatorVisionSystem.getInstance();
            BaseStation.getInstance().getVisionManager().addVisionSystem(simvs);
        }

        // Routes

        post("/addBot", (req,res) -> {
            String body = req.body();
            JsonObject addInfo = jp.parse(body).getAsJsonObject(); // gets (ip, port) from js

            /* storing json objects into actual variables */
            String ip = addInfo.get("ip").getAsString();
            int port = addInfo.get("port").getAsInt();
            String name = addInfo.get("name").getAsString();
            String type = addInfo.get("type").getAsString(); //differentiate between modbot and minibot

            System.out.println(ip);
            System.out.println(port);
            System.out.println(name);
            System.out.println(type);

            /* new modbot is created to add */
            Bot newBot;
            if(type.equals("modbot")) {
                IceConnection ice = new IceConnection(ip, port);
                newBot = new ModBot(ice, name);
            } else if(type.equals("minibot")) {
                TCPConnection c = new TCPConnection(ip, port);
                newBot = new MiniBot(c, name);
            }
               else {
                SimBotConnection sbc = new SimBotConnection();
                SimBot simbot;
                simbot = new SimBot(sbc, name, 50, simvs.getWorld(), 0.0f, 0.0f, 1f, 3.6f, true);
                newBot = simbot;

                simvs.importPhysicalObject(simbot.getMyPhysicalObject());

                // Color sensor TODO put somewhere nice
                ColorIntensitySensor colorSensorL = new ColorIntensitySensor((SimBotSensorCenter) simbot.getSensorCenter(),"right",simbot, 5);
                ColorIntensitySensor colorSensorR = new ColorIntensitySensor((SimBotSensorCenter) simbot.getSensorCenter(),"left",simbot, -5);
                ColorIntensitySensor colorSensorM = new ColorIntensitySensor((SimBotSensorCenter) simbot.getSensorCenter(),"center",simbot, 0);
            }

            return BaseStation.getInstance().getBotManager().addBot(newBot);
        });

        post("/addScenario", (req,res) -> {
            String body = req.body();

            JsonObject scenario = jp.parse(body).getAsJsonObject();
            String scenarioBody = scenario.get("scenario").getAsString();
            JsonArray addInfo = jp.parse(scenarioBody).getAsJsonArray();
            for (JsonElement je : addInfo) {
                String type = je.getAsJsonObject().get("type").getAsString();
                int size = je.getAsJsonObject().get("size").getAsInt();
                int angle = je.getAsJsonObject().get("angle").getAsInt();
                int[] position = gson.fromJson(je.getAsJsonObject().get("position")
                        .getAsString(),int[].class);
                String name = Integer.toString(size)+Integer.toString(angle)
                        + Arrays.toString(position);
                PhysicalObject po = new PhysicalObject(name, 100,
                        simvs.getWorld(), (float)position[0],
                        (float)position[1], size, angle);
                simvs.importPhysicalObject(po);
            }
            /* storing json objects into actual variables */

            return addInfo;
        });

        post("/commandBot", (req,res) -> {
            //System.out.println("post to command bot called");
            String body = req.body();
            JsonObject commandInfo = jp.parse(body).getAsJsonObject();

            // gets (botID, fl, fr, bl, br) from json
            String botName = commandInfo.get("name").getAsString();
            int fl = commandInfo.get("fl").getAsInt();
            int fr = commandInfo.get("fr").getAsInt();
            int bl = commandInfo.get("bl").getAsInt();
            int br = commandInfo.get("br").getAsInt();

            // Forward the command to the bot
            Bot myBot = BaseStation.getInstance().getBotManager()
                    .getBotByName(botName).get();
            FourWheelMovement fwmCommandCenter = (FourWheelMovement) myBot.getCommandCenter();
            return fwmCommandCenter.setWheelPower(fl,fr,bl,br);
        });

        post("/removeBot", (req,res) -> {
            String body = req.body();
            JsonObject removeInfo = jp.parse(body).getAsJsonObject();

            String name = removeInfo.get("name").getAsString();

            return BaseStation.getInstance().getBotManager().removeBotByName(name);
        });


        /**
         * GET /sendScript sends script to the bot identified by botName
         *
         * @apiParam name the name of the bot
         * @apiParam script the full string containing the script
         * @return true if the script sending should be successful
         */
        post("/sendScript", (req,res) -> {
            String body = req.body();
            JsonObject commandInfo = jp.parse(body).getAsJsonObject();

            /* storing json objects into actual variables */
            String name = commandInfo.get("name").getAsString();
            String script = commandInfo.get("script").getAsString();

            Bot receiver = BaseStation.getInstance()
                    .getBotManager()
                    .getBotByName(name)
                    .orElseThrow(NoSuchElementException::new);

            if (receiver instanceof SimBot)
                ((SimBot)BaseStation.getInstance()
                        .getBotManager()
                        .getBotByName(name)
                        .orElseThrow(NoSuchElementException::new)).resetServer();

            return BaseStation.getInstance()
                    .getBotManager()
                    .getBotByName(name)
                    .orElseThrow(NoSuchElementException::new)
                    .getCommandCenter().sendKV("SCRIPT",script);
        });

        get("/trackedBots", (req, res) -> {
            Collection<Bot> allBots = BaseStation.getInstance().getBotManager().getAllTrackedBots();
            return gson.toJson(allBots);
        });

        /**
            Collects updated JSON objects in the form:
            {   "x": vo.coord.x,
                "y": vo.coord.y,
                "angle": vo.coord.getThetaOrZero(),
                "id": vo.id
            }
         */
        get("/updateloc", (req, res) -> {
            // Locations of all active bots
            List<VisionObject> vol = BaseStation
                    .getInstance()
                    .getVisionManager()
                    .getAllLocationData();
            JsonArray respData = new JsonArray();
            for (VisionObject vo : vol) {
                JsonObject jo = new JsonObject();
                jo.addProperty("x", vo.coord.x);
                jo.addProperty("y", vo.coord.y);
                jo.addProperty("angle", vo.coord.getThetaOrZero());
                jo.addProperty("id", vo.vid);
                jo.addProperty("size",vo.size);
                respData.add(jo);
            }
            return respData;

        });

        post("/discoverBots", (req, res) -> {
            return gson.toJson(BaseStation.getInstance().getBotManager().getAllDiscoveredBots());
        });

        post("/runXbox", (req, res) -> {
            String body = req.body();
            JsonObject commandInfo = jp.parse(body).getAsJsonObject();

            /* storing json objects into actual variables */
            String name = commandInfo.get("name").getAsString();

            // if this is called for the first time, initialize the Xbox
            // Controller
            if (xboxControllerDriver == null) {
                // xbox not initialized, initialize it first
                xboxControllerDriver = new XboxControllerDriver();
                // xboxControllerDriver != null
                if (xboxControllerDriver.xboxIsConnected()) {
                    // xbox is connected
                    // run the driver
                    xboxControllerDriver.getMbXboxEventHandler().setBotName
                            (name);
                    xboxControllerDriver.runDriver();
                    return true;
                } else {
                    // xbox is not connected, stop the driver
                    stopXboxDriver();
                    return false;
                }
            } else {
                // xboxControllerDriver != null -- xbox initialized already
                if (xboxControllerDriver.xboxIsConnected()) {
                    // xbox is connected
                    // should be already listening in this case
                    // just set the new name
                    xboxControllerDriver.getMbXboxEventHandler().setBotName
                            (name);
                    return true;
                } else {
                    // xbox is not connected, stop the driver
                    stopXboxDriver();
                    return false;
                }
            }
        });

        post("/stopXbox", (req, res) -> {
            // received stop command, stop the driver
            try{
                stopXboxDriver();
                // no error
                return true;
            } catch (Exception e) {
                // error encountered
                return false;
            }
        });

        // Examplescript routes
        get("/example/patrol/run", (req, res) -> {
            Patrol mbsd = new Patrol( (FourWheelMovement)
                    BaseStation
                            .getInstance().getBotManager().getAllTrackedBots()
                            .iterator().next().getCommandCenter());
            mbsd.start();
            return true;
        });

        get("/example/patrol/addPointToQueue", (req, res) -> {
            patrolPoints.add(BaseStation.getInstance().getVisionManager()
                    .getAllLocationData().get(0).coord);
            return true;
        });

        get("/example/GoBot/addPointToInnerTrack", (req, res) -> {
            innerTrackCoords.add(BaseStation.getInstance().getVisionManager()
                    .getAllLocationData().get(0).coord);
            System.out.println("InnerTrack: " + BaseStation.getInstance().getVisionManager()
                    .getAllLocationData().get(0).coord.x + ", " + BaseStation.getInstance().getVisionManager()
                    .getAllLocationData().get(0).coord.y);
            return true;
        });

        get("/example/GoBot/addPointToOuterTrack", (req, res) -> {
            outerTrackCoords.add(BaseStation.getInstance().getVisionManager()
                    .getAllLocationData().get(0).coord);
            System.out.println("OuterTrack: " + BaseStation.getInstance()
                    .getVisionManager()
                    .getAllLocationData().get(0).coord.x + ", " + BaseStation.getInstance().getVisionManager()
                    .getAllLocationData().get(0).coord.y);
            return true;
        });

        get("/example/GoBot/addPointToStartArea", (req, res) -> {
            startAreaCoords.add(BaseStation.getInstance().getVisionManager()
                    .getAllLocationData().get(0).coord);
            System.out.println("StartArea: " + BaseStation.getInstance()
                    .getVisionManager()
                    .getAllLocationData().get(0).coord.x + ", " + BaseStation.getInstance().getVisionManager()
                    .getAllLocationData().get(0).coord.y);
            return true;
        });

        /*get("/example/GoBot/addPointToFinishArea", (req, res) -> {
            finishAreaCoords.add(BaseStation.getInstance().getVisionManager()
                    .getAllLocationData().get(0).coord);
            return true;
        });*/

        get("/example/GoBot/addPointToMiddleArea", (req, res) -> {
            middleAreaCoords.add(BaseStation.getInstance().getVisionManager()
                    .getAllLocationData().get(0).coord);
            System.out.println("MiddleArea: " + BaseStation.getInstance()
                    .getVisionManager()
                    .getAllLocationData().get(0).coord.x + ", " + BaseStation.getInstance().getVisionManager()
                    .getAllLocationData().get(0).coord.y);
            return true;
        });

        get("/example/GoBot/isBotInside", (req, res) -> course.isInsideTrack(BaseStation.getInstance().getVisionManager()
                .getAllLocationData().get(0).coord));

        /*get("/example/GoBot/didBotFinishALap", (req, res) -> {
            if (gb.finishedLap()) {
                gb.addLapTime(System.nanoTime());
                return "Lap Time: " + gb.getLastLapTime();
            } else {
                return "Current Time: " + System.nanoTime();
            }
        });*/

        get("/example/GoBot/numLapsCompleted", (req, res) -> gb.getLapsDone());

        get("/example/GoBot/startRace", (req, res) -> {
            course.setOuter(outerTrackCoords);
            course.setInner(innerTrackCoords);
            course.setStartArea(startAreaCoords);
            course.setMiddleArea(middleAreaCoords);
            gb.setCourse(course);
            gb.setStartTime(System.nanoTime());
            gb.start();
            return System.nanoTime();
        });


    }

    /**
     * Tells the Xbox Controller Driver to stop
     * Acts as a middleman between this interface and the driver on stopping
     */
    private static void stopXboxDriver() {
        if (xboxControllerDriver != null) {
            // xbox is currently initialized
            xboxControllerDriver.stopDriver();
            xboxControllerDriver = null;
        }
        // xboxControllerDriver == null
        // might get this request from stopXbox HTTP post
    }
}