import basestation.BaseStation;
import basestation.bot.commands.FourWheelMovement;
import basestation.bot.connection.TCPConnection;
import basestation.bot.robot.Bot;
import basestation.bot.robot.minibot.MiniBot;
import org.junit.*;
import static org.junit.Assert.*;
import static spark.Spark.port;
import static spark.Spark.staticFiles;
import org.junit.runners.MethodSorters;
import simulator.baseinterface.SimulatorVisionSystem;
import simulator.physics.PhysicalObject;
import simulator.simbot.SimBot;
import simulator.simbot.SimBotConnection;
import spark.route.RouteOverview;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BaseHTTPInterfaceTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        try {
            ProcessBuilder pb = new ProcessBuilder("python", "python-interface/src/tcp.py");
            pb.start();
        }
        catch(Exception e){
            e.printStackTrace();
        }
        System.out.println("opening TCP");
        Thread.sleep(5000);
    }

    @Test
    public void testAddBot() throws Exception {
        Bot testMiniBot;
        Bot testSimBot;
        String ip = "127.0.0.1";
        int port = 10000;
        port(8080);
        staticFiles.location("/public");
        RouteOverview.enableRouteOverview("/");
        SimulatorVisionSystem simvs;
        simvs = SimulatorVisionSystem.getInstance();
        assertNotNull(simvs);
        assertNotEquals(BaseStation.getInstance().getVisionManager().addVisionSystem(simvs), 0);

        TCPConnection c = new TCPConnection(ip, port);
        testMiniBot = new MiniBot(c, "TestCaseMiniBot");
        assertEquals(c.connectionActive(), true);

        SimBotConnection sbc = new SimBotConnection();
        assertEquals(sbc.connectionActive(), true);
        PhysicalObject po = new PhysicalObject("TESTBOT", 50, simvs.getWorld(), 0.4f, 0.0f, 1f, 1f, true);

        SimBot smbot;
        smbot = new SimBot(sbc, "TestCaseSimBot", po);
        testSimBot = smbot;

        ArrayList<PhysicalObject> pObjs = new ArrayList<>();
        pObjs.add(po);
        simvs.processPhysicalObjects(pObjs);

        assertEquals(BaseStation.getInstance().getBotManager().addBot(testMiniBot), "TestCaseMiniBot");
        assertEquals(BaseStation.getInstance().getBotManager().addBot(testSimBot), "TestCaseSimBot");
        assertTrue(BaseStation.getInstance().getBotManager().getBotByName("TestCaseMiniBot").isPresent());
        assertTrue(BaseStation.getInstance().getBotManager().getBotByName("TestCaseSimBot").isPresent());
        assertEquals(simvs.getAllPhysicalObjects().size(), 1);

    }
    @Test
    public void testCommandBot() {
        boolean isPresent = BaseStation.getInstance().getBotManager().getBotByName("TestCaseMiniBot").isPresent();
        Bot testMiniBot = null;
        if(isPresent){
            testMiniBot = BaseStation.getInstance().getBotManager().getBotByName("TestCaseMiniBot").get();
        }
        FourWheelMovement fwmCommandCenter = (FourWheelMovement) testMiniBot.getCommandCenter();
        int fl = 50, fr = 50, bl = 50, br = 50;
        assertEquals(fwmCommandCenter.setWheelPower(fl,fr,bl,br), true);
    }

    @Test
    public void testRemoveBot() {
        assertEquals(BaseStation.getInstance().getBotManager().removeBotByName("TestCaseMiniBot").isPresent(), true);
        assertEquals(BaseStation.getInstance().getBotManager().getBotByName("TestCaseMiniBot").isPresent(), false);
    }

    @AfterClass
    public static void tearDownClass() {
//        try {
//            BufferedReader reader =
//                    new BufferedReader(new FileReader("././python-interface/src/close_tcp.py"));
//            String header = "";
//            String sCurrentLine;
//            while ((sCurrentLine = reader.readLine()) != null) {
//                header = header + sCurrentLine + "\n";
//            }
//            BufferedWriter scriptOut = new BufferedWriter(new FileWriter("test_close_tcp.py"));
//            scriptOut.write(header);
//            scriptOut.close();
//
//            ProcessBuilder pb = new ProcessBuilder("python", "test_close_tcp.py");
//            pb.start();
//        }
//        catch(Exception e){
//            System.out.println(e);
//        }

//        System.out.println("closing TCP");
    }
}
