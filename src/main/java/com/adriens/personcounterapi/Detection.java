package com.adriens.personcounterapi;

/**
 * Class for Detected Objects
 */
public class Detection {
    private int id;
    private String className;
    private String alias;
    private double probability;
    private double x;
    private double y;
    private double width;
    private double height;

    /**
     * Creates a Detection object
     * @param id the id of the object
     * @param className class name of the object
     * @param probability probability of the detection
     * @param x x coordinates of object
     * @param y y coordinates of object
     * @param width width of the object
     * @param height height of the object
     */
    public Detection(int id, String className, String alias, double probability, double x, double y, double width, double height) {
        this.id = id;
        this.className = className;
        this.alias = alias;
        this.probability = probability;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Returns the detection's id
     * @return the detection's id
     */
    public int getId(){
        return id;
    }

    /**
     * Returns the detection's class name
     * @return the class name of the detection
     */
    public String getClassName(){
        return className;
    }

    public String getAlias(){
        return alias;
    }

    /**
     * Returns the detection's probability
     * @return the detection's probability
     */
    public double getProbability(){
        return probability;
    }

    /**
     * Returns the detection x coordinate
     * @return the detection x coordinate
     */
    public double getX(){
        return x;
    }

    /**
     * Returns the detection y coordinate
     * @return the detection y coordinate
     */
    public double getY(){
        return y;
    }

    /**
     * Returns the detection width
     * @return the detection width
     */
    public double getWidth(){
        return width;
    }

    /**
     * Returns the detection height
     * @return the detection height
     */
    public double getHeight(){
        return height;
    }
}
