package examples.gobot;

import basestation.BaseStation;
import basestation.bot.commands.FourWheelMovement;
import basestation.vision.VisionCoordinate;
import basestation.vision.VisionObject;
import minibot.BaseHTTPInterface;

import java.util.ArrayList;
import java.util.List;

public class GoBot extends Thread {

    private final int NUM_LAPS = 1;
    private final AIUtil ai;
    private Course course;
    private int lapsDone;
    private boolean reachedMiddle;
    private boolean crossedLapLine;
    private long startTime;
    private ArrayList<Long> lapTimes;
    private boolean inTrack;
    private int botState;
    private int lastBotState;
    private float humanTime;
    private float botTime;
    public static final int WAITING = 0;
    public static final int HUMAN_PLAYING = 1;
    public static final int BOT_PLAYING = 2;


    private final Navigator navigator;
    FourWheelMovement fwm;
    int index;

    public GoBot() {
        this.lapsDone = 0;
        this.crossedLapLine = false;
        this.reachedMiddle = false;
        this.startTime = 0L;
        this.inTrack = true;
        this.lapTimes = new ArrayList<>();
        this.course = new Course();
        this.botState = WAITING; //waiting state
        this.lastBotState = -1;

        this.ai = new AIUtil();

        this.fwm = fwm;
        this.navigator = new Navigator();
        navigator.start();
    }

    public void setCourse(Course c) {
        this.course = c;
        this.ai.course = c;
    }

    public void setStartTime(long time) {
        this.startTime = time;
    }

    public long getStartTime() {
        return this.startTime;
    }

    public int getLapsDone() {
        return this.lapsDone;
    }

    public ArrayList<Double> convertNanoTimeLapsToSeconds() {
        ArrayList<Double>lapTimeInSec = new ArrayList<>();
        for(int i = 0; i < lapTimes.size();i++) {
            if(i == 0) {
                lapTimeInSec.add((double)(lapTimes.get(0)-getStartTime())/1000000000);
            } else {
                lapTimeInSec.add((double)(lapTimes.get(i)-lapTimes.get(i-1))/1000000000);
            }
        }
        return lapTimeInSec;
    }

    public String lapTimes() {
        ArrayList<Double> lapTimeInSec = convertNanoTimeLapsToSeconds();
        String output = "";
        for(int i = 0; i < lapTimeInSec.size(); i++) {
            output+= "Lap " + (i+1) + ": " + lapTimeInSec.get(i) + "seconds\n";
        }
        return output;
    }

    public float totalTime() {
        float sum = 0;
        for(int i = 0; i < lapTimes.size(); i++) {
            if(i == 0) {
                sum+= lapTimes.get(i) - this.startTime;
            } else
            sum += (lapTimes.get(i) - lapTimes.get(i-1));
        }
        return sum/1000000000;
    }

    public void setBotState(int state) {
        this.botState = state;
    }

    private void printState() {
        System.out.printf("HUMAN: %s, AI: %s", humanTime, botTime);
    }

    @Override
    public void run() {
        //this.setTimer(System.nanoTime());
        this.fwm = (FourWheelMovement)
                BaseStation
                        .getInstance().getBotManager().getAllTrackedBots()
                        .iterator().next().getCommandCenter();

        while (true) {
            if (lastBotState != botState) {
                this.startTime = System.nanoTime();
            }
            lastBotState = botState;
            if (botState == HUMAN_PLAYING) {
                if (lapsDone >= NUM_LAPS) {
                    // Finished!
                    botState = WAITING;
                    humanTime = totalTime();
                    lapTimes.clear();
                    lapsDone = 0;
                    printState();
                } else {
                    List<VisionObject> vl =  BaseStation.getInstance().getVisionManager()
                            .getAllLocationData();
                    if (vl.size() != 0) {
                        VisionCoordinate vc = vl.get(0).coord;
                        if (this.course.isInsideTrack(vc)) {
                            inTrack = true;
                            if (course.getStartArea().contains(vc.x, vc.y)) {
                                if (!crossedLapLine && !reachedMiddle) {
                                    this.crossedLapLine = true;
                                } else if (crossedLapLine && reachedMiddle) {
                                    this.lapsDone++;
                                    this.reachedMiddle = false;
                                    this.lapTimes.add(System.nanoTime());
                                }
                            } else if (course.getMiddleArea().contains(vc.x, vc.y)) {
                                if (!reachedMiddle) {
                                    this.reachedMiddle = true;
                                }
                            }
                        } else {
                            //do stuff with timer later to tell to get back
                            System.out.println("go back inside pls");
                            inTrack = false;
                        }
                    }
                }
            }
            else if (botState == BOT_PLAYING) { //ASSUMING CCL TRACK
                if (lapsDone >= NUM_LAPS) {
                    // Finished!
                    botState = WAITING;
                    botTime = totalTime();
                    lapTimes.clear();
                    lapsDone = 0;
                    printState();
                } else {
                    List<VisionObject> vl =  BaseStation.getInstance().getVisionManager()
                            .getAllLocationData();
                    if (vl.size() != 0) {
                        VisionCoordinate vc = vl.get(0).coord;
                        inTrack = this.course.isInsideTrack(vc);
                        if (navigator.destinationReached()) {
                            //int max = BaseHTTPInterface.innerTrackCoords
                            // .size(); for tracing inner track
                            int max = BaseHTTPInterface.advancedAI.size();
                            if (max != 0) {
                                //navigator.goToDestination(BaseHTTPInterface
                                // .innerTrackCoords.get(index)); for tracing
                                // inner track
                                navigator.goToDestination(BaseHTTPInterface
                                        .advancedAI.get(index));
                                System.out.println(navigator.destination);
                                index = (index + 1) % max;
                            }
                        }
                        if (course.getStartArea().contains(vc.x, vc.y)) {
                            if (!crossedLapLine && !reachedMiddle) {
                                this.crossedLapLine = true;
                            } else if (crossedLapLine && reachedMiddle) {
                                this.lapsDone++;
                                this.reachedMiddle = false;
                                this.lapTimes.add(System.nanoTime());
                                System.out.println(lapTimes());
                            }
                        } else if (course.getMiddleArea().contains(vc.x, vc.y)) {
                            if (!reachedMiddle) {
                                this.reachedMiddle = true;
                            }
                        }
                    }
                }

            } else {
                //System.err.println("UNKNOWN STATE");
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private class Navigator extends Thread {
        private boolean destinationReached;
        private VisionCoordinate destination;

        private static final double DISTANCE_THRESHOLD = 0.08;
        private static final double ANGLE_THRESHOLD = Math.PI/(10);
        private static final int MAX_SPEED = 100;
        private static final int MIN_SPEED = 50;

        @Override
        public void run() {
            while (true) {
                calcRoute();
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public boolean destinationReached() {
            return destination == null || destinationReached;
        }

        public void goToDestination(VisionCoordinate vc) {
            this.destination = vc;
            destinationReached = false;
        }

        private double mod(double a, double n){
            return a - Math.floor(a/n)*n;
        }

        public void calcRoute(){
            if (destinationReached()) return;
            if(destination == null){
                return;
            }

            VisionCoordinate vc;
            List<VisionObject> locs = BaseStation.getInstance()
                    .getVisionManager
                            ().getAllLocationData();

            if (locs.size() == 0 || locs.get(0) == null){
                vc = null;
                fwm.setWheelPower(0,0,0,0);
                return;
            }
            else {
                vc = locs.get(0).coord;
                //System.out.println(vc);
            }

            double spectheta = vc.getThetaOrZero();
            double toAngle = vc.getAngleTo(destination);
            double angle = mod((toAngle - spectheta + Math.PI), 2*Math.PI);
            double dist = vc.getDistanceTo(destination);
            System.out.println("ang: " + angle + ", toAngle:" + toAngle);
            if (!false) {
                return;
            }

            // driver

            //if (!inTrack) return;
            if (false) {
                double driveAngle = ai.calculateDriveAngle();
                double MIDDLE = Math.PI / 2;
                double QUARTER = MIDDLE - MIDDLE * .1;
                double THREEQUARTER = MIDDLE + MIDDLE * .1;
                int POWER = 70;
                if (driveAngle < QUARTER) {
                    fwm.setWheelPower(POWER,-POWER,POWER,-POWER);
                } else if (driveAngle < THREEQUARTER) {
                    fwm.setWheelPower(POWER,POWER,POWER,POWER);
                } else {
                    fwm.setWheelPower(-POWER,POWER,-POWER,POWER);
                }
            } else {
                if (dist > DISTANCE_THRESHOLD) {
                    if (Math.abs(angle) > ANGLE_THRESHOLD) {
                        // Need to rotate to face destination

                        // Calculate angular speed
                        double angSpeed = MIN_SPEED;
                        if (Math.abs(angle) > Math.toRadians(20)) {
                            angSpeed = MIN_SPEED + (MAX_SPEED - MIN_SPEED) *
                                    (Math.abs(angle) - Math.toRadians(20)) /
                                    Math.toRadians(160.);
                        }

                        if (angSpeed > MAX_SPEED) angSpeed = MAX_SPEED;

                        // Rotate in proper direction
                        if (angle < 0) {
                            fwm.setWheelPower(angSpeed,
                                    -angSpeed,angSpeed,-angSpeed);
                            //System.out.println("turn CCW");

                        } else {
                            fwm.setWheelPower(-angSpeed,
                                    angSpeed,-angSpeed,angSpeed);

                            //System.out.println("turn CW");
                        }
                    } else {
                        if (dist > DISTANCE_THRESHOLD) {
                            // Facing destination, need to move forward

                            // Calculate Forward speed
                            double speed = MIN_SPEED;
                            if (dist > 0.2) {
                                speed = MIN_SPEED + (MAX_SPEED - MIN_SPEED) * (dist - 0.2) * 3;
                            }

                            if (speed > MAX_SPEED) speed = MAX_SPEED;

                            // Move forward
                            fwm.setWheelPower(speed, speed,
                                    speed, speed);
                        } else {
                            fwm.setWheelPower(0,0,0,0);
                            destinationReached = true;
                        }
                    }
                } else {
                    fwm.setWheelPower(0,0,0,0);
                    destinationReached = true;
                }
            }
        }
    }
}
