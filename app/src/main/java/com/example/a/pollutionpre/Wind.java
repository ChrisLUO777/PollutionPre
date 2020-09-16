package com.example.a.pollutionpre;

public class Wind {
    private int deg=0;
    private int spd=0;
    private double kel=-273.15;

    public Wind(int inputdeg, int inputspd, double inputkel){
        this.deg=inputdeg;
        this.spd=inputspd;
        this.kel=inputkel;
    }
    public int getDeg(){
        return this.deg;
    }
    public void setDeg(int inputdeg){
        this.deg=inputdeg;
    }
    public int getSpd(){
        return this.spd;
    }
    public void setSpd(int inputspd){
        this.spd=inputspd;
    }
    public double getKel(){
        return this.kel;
    }
    public void setKel(double inputkel){
        this.kel=inputkel;
    }
}
