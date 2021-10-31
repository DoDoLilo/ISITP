package cn.whu.cs.niu.PDR;

public class CoordinateTool {


    static public void updateCoordinate(double[] grv, double[][] positions){
        double azimuth = grvToAzimuth(grv);
        nrotateFromStart(azimuth, positions);
        translationPosition(positions);
    }

    static private double grvToAzimuth(double[] grv){
        double x = grv[0];
        double y = grv[1];
        double z = grv[2];
        double w = grv[3];

        return Math.atan2(2 * (w * z + x * y), 1 - 2 * (z * z + y * y));
    }

    static private void nrotateFromStart(double azimuth, double[][] positions){
        double[] startPoint = new double[]{0, 0};
        for(double[] position : positions){
            double x = position[0], y = position[1];
            position[0] = (x - startPoint[0]) * Math.cos(azimuth) -
                    (y - startPoint[1]) * Math.sin(azimuth) + startPoint[0];
            position[1] = (x - startPoint[0]) * Math.sin(azimuth) +
                    (y - startPoint[1]) * Math.cos(azimuth) + startPoint[1];
        }
    }
    static private void translationPosition(double[][] positions){
        double[] startPoint = new double[]{0, 0};
        int lastIndex = positions.length - 1;
        double xDistance = startPoint[0] - positions[lastIndex][0];
        double yDistance = startPoint[1] - positions[lastIndex][1];
        for(double[] position : positions){
            position[0] += xDistance;
            position[1] += yDistance;
        }
    }
}
