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

        return rand.nextFloat() * (maxX - minX) + 0.9f * minX;
    }

    private static boolean IsValidCoord(float coord, double min, double max)
    {
        if (coord < min || coord > max) return true;
        else return false;
    }

}
