package simulator.simbot;

import basestation.bot.commands.FourWheelMovement;
import simulator.baseinterface.SimulatorVisionSystem;
import org.jbox2d.dynamics.Body;
import org.jbox2d.common.Vec2;

public class SimBotCommandCenter implements FourWheelMovement {
    public final float topspeed = 0.655f; //top speed of minibot in meters/second
    public final float turningspeed = (float) Math.PI;
    public SimBotCommandCenter(SimBot myBot) {
    }

    @Override
    public boolean setWheelPower(double fl, double fr, double bl, double br) {
        Body b = SimulatorVisionSystem.getInstance().getWorld().getBodyList();

        //forwards
        if(fl > 0 && fr > 0 && bl >0 && br>0) {
            System.out.println("forward");
            float angle = b.getAngle();
            float newX = (float) (topspeed*fl/100*Math.cos(angle));
            float newY = (float) (topspeed*fl/100*Math.sin(angle));
            b.setLinearVelocity(new Vec2(newX, newY));
            b.setAngularVelocity(0.0f);
        }

        //backwards
        if(fl < 0 && fr < 0 && bl < 0 && br < 0) {
            float angle = b.getAngle();
            float newX = (float) (topspeed*fl/100*Math.cos(angle));
            float newY = (float) (topspeed*fl/100*Math.sin(angle));
            System.out.println("backward");
            b.setLinearVelocity(new Vec2(newX, newY));
            b.setAngularVelocity(0.0f);
        }

        //no motor power
        if(fl == 0 && fr == 0 && bl == 0 && br == 0) {
            System.out.println("no motor power");
            b.setLinearVelocity(new Vec2(0.0f, 0.0f));
            b.setAngularVelocity(0.0f);
        }

        //turning right
        if(fl > 0 && fr < 0 && bl > 0 && br < 0) {
            System.out.println("turning right");
            b.setLinearVelocity(new Vec2(0.0f, 0.0f));
            b.setAngularVelocity(-turningspeed);
        }

        //turning left
        if(fl < 0 && fr > 0 && bl < 0 && br > 0) {
            System.out.println("turning left");
            b.setLinearVelocity(new Vec2(0.0f, 0.0f));
            b.setAngularVelocity(turningspeed);
        }

        return true;
    }

    @Override
    public boolean sendKV(String key, String value) {
        return false;
    }
}
