package minibot;

import basestation.BaseStation;
import basestation.bot.commands.CommandCenter;
import basestation.bot.commands.FourWheelMovement;
import basestation.bot.connection.IceConnection;
import basestation.bot.connection.TCPConnection;
import basestation.bot.robot.Bot;
import basestation.bot.robot.minibot.MiniBot;
import basestation.bot.robot.modbot.ModBot;
import basestation.vision.OverheadVisionSystem;
import basestation.vision.VisionObject;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.jbox2d.dynamics.World;
import simulator.Simulator;
import simulator.physics.PhysicalObject;
import simulator.simbot.*;
import spark.route.RouteOverview;

import java.util.List;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Collection;
import java.io.*;
import java.util.stream.Collectors;


import simulator.baseinterface.SimulatorVisionSystem;


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

    public static void main(String[] args) {
        // Spark configuration
        port(8080);
        staticFiles.location("/public");
        RouteOverview.enableRouteOverview("/");

        //create new visionsystem and simulator instances
        SimulatorVisionSystem simvs;
        Simulator simulator = new Simulator();
        // Show exceptions
        exception(Exception.class, (exception,request,response) -> {
            exception.printStackTrace();
            response.status(500);
            response.body("oops");
        });

        // Global objects
        JsonParser jsonParser = new JsonParser();
        Gson gson = new Gson();

        if (OVERHEAD_VISION) {
            OverheadVisionSystem ovs = new OverheadVisionSystem();
            BaseStation.getInstance().getVisionManager().addVisionSystem(ovs);
            simvs = simulator.getVisionSystem();
            BaseStation.getInstance().getVisionManager().addVisionSystem(simvs);
        }

        // Routes
        /* add a new bot from the gui*/
        post("/addBot", (req,res) -> {
            String body = req.body();
            JsonObject addInfo = jsonParser.parse(body).getAsJsonObject(); // gets (ip, port) from js

            /* storing json objects into actual variables */
            String ip = addInfo.get("ip").getAsString();
            int port = addInfo.get("port").getAsInt();
            String name = addInfo.get("name").getAsString();
            String type = addInfo.get("type").getAsString(); //differentiate between modbot and minibot

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
                simbot = new SimBot(sbc, simulator, name, 50, simulator.getWorld
                        (), 0.0f,
                        0.0f, 1f, 3.6f, 0, true);
                newBot = simbot;

                simulator.importPhysicalObject(simbot.getMyPhysicalObject());

                // Color sensor TODO put somewhere nice
                ColorIntensitySensor colorSensorL = new ColorIntensitySensor((SimBotSensorCenter) simbot.getSensorCenter(),"right",simbot, 5);
                ColorIntensitySensor colorSensorR = new ColorIntensitySensor((SimBotSensorCenter) simbot.getSensorCenter(),"left",simbot, -5);
                ColorIntensitySensor colorSensorM = new ColorIntensitySensor((SimBotSensorCenter) simbot.getSensorCenter(),"center",simbot, 0);
            }
            return BaseStation.getInstance().getBotManager().addBot(newBot);
        });


        /**
         * POST /addScenario starts a simulation with the scenario from the
         * scenario viewer in the gui
         *
         * @apiParam scenario a string representing a list of JSON scenario
         * objects, which consist of obstacles and bot(s)
         * @return the scenario json if it was successfully added
         */
        post("/addScenario", (req,res) -> {

            String body = req.body();
            for (String name : BaseStation.getInstance().getBotManager().getAllTrackedBots().stream().map(Bot::getName).collect(Collectors.toList())){
                BaseStation.getInstance().getBotManager().removeBotByName(name);
            }


            return simulator.importScenario(gson, jsonParser, jsonParser.parse(body).getAsJsonObject());

        });

        /**
         * POST /saveScenario saves the scenario currently loaded as a txt
         * file with the specified name
         *
         * @apiParam scenario a string representing a list of JSON scenario
         * objects, which consist of obstacles and bot(s)
         * @apiParam name the name the new scenario txt file
         * @return the name of the file if it was successfully saved
         */
        post("/saveScenario", (req,res) -> {

            String body = req.body();
            JsonObject scenario = jsonParser.parse(body).getAsJsonObject();
            String scenarioBody = scenario.get("scenario").getAsString();
            String fileName = scenario.get("name").getAsString();

            //writing new scenario file
            File file = new File
                    ("cs-minibot-platform-src/src/main/resources" +
                            "/public/scenario/"+fileName+".txt");
            OutputStream out = new FileOutputStream(file);

            FileWriter writer = new FileWriter(file, false);
            BufferedWriter bwriter = new BufferedWriter(writer);
            bwriter.write(scenarioBody);
            bwriter.close();
            out.close();
            return fileName;
        });

        /**
         * POST /loadScenario loads a scenario into the scenario viewer from a
         * txt scenario file with the specified name; does not add scenario
         * to the world or start a simulation
         *
         * @apiParam name the name the scenario txt file to load
         * @return the JSON of the scenario if it was loaded successfully
         */
        post("/loadScenario", (req,res) -> {
            String body = req.body();
            JsonObject scenario = jsonParser.parse(body).getAsJsonObject();
            String fileName = scenario.get("name").getAsString();
            String scenarioData = "";

            //loading scenario file
            File file = new File
                    ("cs-minibot-platform-src/src/main/resources" +
                            "/public/scenario/"+fileName+".txt");
            FileReader fr=	new	FileReader(file);
            BufferedReader br=	new	BufferedReader(fr);
            String line = br.readLine();
            while (line!=null){
                scenarioData+=line;
                line = br.readLine();
            }
            br.close();
            return scenarioData;
        });

        /*send commands to the selected bot*/
        post("/commandBot", (req,res) -> {
            System.out.println("post to command bot called");
            String body = req.body();
            JsonObject commandInfo = jsonParser.parse(body).getAsJsonObject();

            // gets (botID, fl, fr, bl, br) from json
            String botName = commandInfo.get("name").getAsString();
            String fl = commandInfo.get("fl").getAsString();
            String fr = commandInfo.get("fr").getAsString();
            String bl = commandInfo.get("bl").getAsString();
            String br = commandInfo.get("br").getAsString();

            // Forward the command to the bot
            Bot myBot = BaseStation.getInstance().getBotManager()
                    .getBotByName(botName).get();
            CommandCenter cc =  myBot.getCommandCenter();
            return cc.sendKV("WHEELS", fl + "," + fr + "," + bl + "," + br);
        });

        /*remove the selected bot -  not sure if still functional*/
        post("/removeBot", (req,res) -> {
            String body = req.body();
            JsonObject removeInfo = jsonParser.parse(body).getAsJsonObject();
            String name = removeInfo.get("name").getAsString();
            return BaseStation.getInstance().getBotManager().removeBotByName(name);
        });

        post( "/logdata", (req,res) -> {
            String body = req.body();
            JsonObject commandInfo = jsonParser.parse(body).getAsJsonObject();
            String name = commandInfo.get("name").getAsString();
            Bot myBot = BaseStation.getInstance().getBotManager().getBotByName(name).get();
            CommandCenter cc = myBot.getCommandCenter();
            if (!cc.isLogging()) {
                System.out.println("Start Logging");
            } else {
                System.out.println("Stop Logging");
            }
            cc.toggleLogging();
            return true;
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
            JsonObject commandInfo = jsonParser.parse(body).getAsJsonObject();

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

        /**
         * GET /sendKV sends script to the bot identified by botName
         *
         * @apiParam name the name of the bot
         * @apiParam script the full string containing the script
         * @return true if the script sending should be successful
         */
        post("/sendKV", (req,res) -> {
            String body = req.body();
            JsonObject commandInfo = jsonParser.parse(body).getAsJsonObject();

            String kv_key = commandInfo.get("key").getAsString();
            String kv_value = commandInfo.get("value").getAsString();
            String name = commandInfo.get("name").getAsString();
            System.out.println(name);

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
                    .getCommandCenter().sendKV(kv_key,kv_value);
        });

        post("/discoverBots", (req, res) -> {
            return gson.toJson(BaseStation.getInstance().getBotManager().getAllDiscoveredBots());
        });

        post("/getOccupancyMatrix", (req, res) -> {
            //Thread.sleep(5000);
            String body = req.body();
            JsonObject settings = jsonParser.parse(body).getAsJsonObject();
            String height = settings.get("height").getAsString();
            String width = settings.get("width").getAsString();
            String size = settings.get("size").getAsString();
            System.out.println(height + ", " + width + ", " + size);
            simulator.generateOccupancyMatrix(Integer.parseInt(height), Integer.parseInt(width), Float.parseFloat(size));
            int[][] path = simulator.getDijkstras();
                    for (int j = 1; j < path.length; j++) {
            for (int i = 1; i < path[j].length; i++) {
                System.out.print(path[i][j] + " ");
            }
            System.out.println();
        }
            //System.out.println("getOccupancyMatrix");
            return gson.toJson(simulator.getOccupancyMatrix());
        });

        post( "/getDijkstras", (req, res) -> {
            return gson.toJson(simulator.getDijkstras());
        });

        post("/runXbox", (req, res) -> {
            String body = req.body();
            JsonObject commandInfo = jsonParser.parse(body).getAsJsonObject();

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
