package examples.gobot;

import basestation.BaseStation;
import basestation.vision.VisionCoordinate;
import basestation.vision.VisionObject;

import java.util.ArrayList;
import java.util.List;

public class AIUtil {

    private final int numLines;
    private final double min;
    private final double max;
    Course course;

    public AIUtil() {
        this.numLines = 5;
        this.min = 0+.01;
        this.max = Math.PI-0.01;
    }

    //Find intersection between two equations
    public Point intersection(Equation e1, Equation e2){
        try{
            //two vertical lines cannot intersect each other (unless they are
            // the same, which should only occur if the car is driving on the
            // boundary
            if (e1.vertical && e2.vertical){
                return null;
            }
            //If either are vertical, just plug in
            else if (e1.vertical || e2.vertical) {
                if (e1.vertical){
                    return new Point(e1.xval, e1.xval * e2.slope +
                            e2.yIntercept);
                }
                else{
                    return new Point(e2.xval, e2.xval * e1.slope +
                            e1.yIntercept);
                }

            }
            else {
                double x = (e2.yIntercept - e1.yIntercept)/(e1.slope -
                        e2.slope);
                double y = e1.slope * x + e1.yIntercept;
                return new Point(x, y);
            }
        }

        //no intersection
        catch (Exception alan) {
            alan.printStackTrace();
        }
        return null;
    }

    //Find all equations between points in order
    public ArrayList<Equation> findEquations(ArrayList<VisionCoordinate> cors){
        int size = cors.size();
        ArrayList<Equation> eqs = new ArrayList<Equation>();
        for (int i = 0; i < size; i++){
            eqs.add(new Equation(cors.get(i), cors.get((i+1)%size)));
        }
        return eqs;
    }

    //find intersections to target equation
    public ArrayList<Point> findIntersection(ArrayList<Equation> e, Equation target){
        int size = e.size();
        ArrayList<Point> pts = new ArrayList<Point>();
        for (int i = 0; i < size; i++){
            Point interPoint = intersection(e.get(i), target);
            if (interPoint != null && e.get(i).inDomain(interPoint)){
                pts.add(interPoint);
            }
        }
        for(int i = 0; i < pts.size(); i++){
//            System.out.println("pt "+ pts.get(i).xcor + ", " + pts.get(i).ycor);
        }
        return pts;

    }

    //Find distance between current location of bot and an intersection point
    public double distanceBetween(VisionCoordinate vc, Point p){
        double dx = vc.x - p.xcor;
        double dy = vc.y - p.ycor;
        return Math.sqrt(dx*dx + dy*dy);
    }

    //return a list of all equations to sweep across
    public ArrayList<Equation> sweep(VisionCoordinate vc, int numLines){
        ArrayList<Equation> sweepLines = new ArrayList<>();
        for (int i =0; i < numLines; i ++){
            sweepLines.add(new Equation(vc, vc.getThetaOrZero() + i *
                    intervalAngle() - (max - min)/2));
        }
        return sweepLines;
    }

    //return the angle between sweep lines
    public double intervalAngle(){
        return (max - min)/(numLines - 1);
    }

    public ArrayList<Double> allAngles(){
        ArrayList<Double> angles = new ArrayList<>();
        for(int i = 0; i < numLines; i++){
            angles.add(min + i*intervalAngle());
        }
        return angles;
    }

    //determine angle between positive x axis to point
    public double pointAngle(VisionCoordinate origin, Point target){
//        double botAngle = origin.getThetaOrZero();
        boolean isVertical = origin.x - target.xcor == 0;
        if (!isVertical){
            double deltaY = target.ycor - origin.y;
            double deltaX = target.xcor - origin.x;
            return Math.atan2(deltaY,deltaX);
        }
        else{
            if (target.ycor > origin.y){
                return Math.PI;
            }
            else{
                return 3*Math.PI;
            }
        }
    }

    //return if point is valid
    public boolean isValid(double pAngle, VisionCoordinate vc, double
            sweepTotalAngle, double
                                   sweepAngle){
        return Math.abs(pAngle - (vc.getThetaOrZero() - (sweepTotalAngle/2) +
                sweepAngle)) < 0.1; // TODO MAGIC ### -t revor
    }


    public ArrayList<Double> calculateRayDistances(ArrayList<Equation> allLines,
                                                   ArrayList<Equation> sweepLines,
                                                   VisionCoordinate botCoordinate){
        ArrayList<Double> distances = new ArrayList<>();
        for (int i = 0; i < sweepLines.size(); i++){
            double minDist = Double.MAX_VALUE / 100f;
            for (Point intersectionPoint: findIntersection(allLines, sweepLines.get(i))){
                if (isValid(pointAngle(botCoordinate, intersectionPoint), botCoordinate, max - min, i*intervalAngle()
                )) {
                    double dist = distanceBetween(botCoordinate, intersectionPoint);
                    if (dist < minDist) {
                        minDist = dist;
                        if (i == 2) {
                            System.out.println(intersectionPoint);
                        }
                    }
                } else {
                    System.err.println("INVALID" + intersectionPoint);
                    System.err.println("INVALID" + pointAngle(botCoordinate,
                            intersectionPoint));
                }
            }
            distances.add(minDist);
        }
        return distances;
    }

    public double calculateDriveAngle(){
        List<VisionObject> v1 = BaseStation.getInstance().getVisionManager()
                .getAllLocationData();
        if (v1.size() != 0){
            VisionCoordinate pos = v1.get(0).coord;
//            System.out.println("Current pos: " + pos + "\n");
            ArrayList<Equation> sweepEquations = sweep(pos, numLines);
            for(Equation f : sweepEquations){
//                System.out.println("Y=" + f.slope + "x + " + f.yIntercept );
            }
            ArrayList<VisionCoordinate> inner = course.getInner().returnCoords();
            ArrayList<VisionCoordinate> outer = course.getOuter()
                    .returnCoords();
            ArrayList<Equation> innerEqs = findEquations(inner);
            ArrayList<Equation> outerEqs = findEquations(outer);
            innerEqs.addAll(outerEqs);
            ArrayList<Double> important = calculateRayDistances(innerEqs, sweepEquations,
                    pos);
//            important.forEach(System.out::println);
            double sumDistances = 0;
            for(double d: important){
                sumDistances += d;
            }
            double theta = max - min;
            double out = 0;
            for(int i = 0; i < numLines; i++){
                out += theta/(numLines-1)*i*(important.get(i))/(sumDistances);
            }
            System.out.println("Angle: " + (out+(Math.PI-theta)/2));
            return out + (Math.PI - theta)/2;

        }
        return 0;
    }
}
