package com.adriens.personcounterapi;

public class Detection {
    private String className;
    private double probability;
    private double x;
    private double y;
    private double width;
    private double height;

    public Detection(int id, String className, double probability, double x, double y, double width, double height) {
        this.className = className;
        this.probability = probability;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public String getClassName(){
        return className;
    }

    public double getProbability(){
        return probability;
    }

    public double getX(){
        return x;
    }

    public double getY(){
        return y;
    }

    public double getWidth(){
        return width;
    }

    public double getHeight(){
        return height;
    }
}
