package com.example.m.kulka2d;

import java.util.Random;

public class Helpers extends Object {

    public static float[] CreateRandomTargetCoordinates(float width, float height
            , double exclusiveX1, double exclusiveY1, double exclusiveX2, double exclusiveY2) {
        float x = CreateRandomCoord(width);
        float y = CreateRandomCoord(height);
        while (!IsValidCoord(x,exclusiveX1,exclusiveX2)){
            x = CreateRandomCoord(width);
        }
        while (!IsValidCoord(y,exclusiveY1,exclusiveY2)){
            y = CreateRandomCoord(height);
        }
        return new float[] {x, y};
    }

    private static float CreateRandomCoord(float measure) {
        float minX = -measure;
        float maxX = measure;

        Random rand = new Random();

        return rand.nextFloat() * (maxX - 0.7f * minX) + 0.7f * minX;
    }

    private static boolean IsValidCoord(float coord, double min, double max)
    {
        if (coord < 1.2f * min || coord > 1.2 * max) return true;
        else return false;
    }

    public static String GetOrientation(float y, float z){
        if (y > 70f) return "V";
        else if (y < -70f) return "^";
        else if (z > 70f) return ">";
        else if (z<-70f) return "<";
        else return"o";

    }

}
