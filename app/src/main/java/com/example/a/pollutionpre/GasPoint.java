package com.example.a.pollutionpre;

public class GasPoint {
    private double lat;
    private double lng;
    private int thick;
    public GasPoint(double lat, double lng, int thick){
        this.lat=lat;
        this.lng=lng;
        this.thick=thick;
    }
    public double getLat(){
        return this.lat;
    }
    public void setLat(double lat){
        this.lat=lat;
    }
    public double getLng(){
        return this.lng;
    }
    public void setLng(double lng){
        this.lng=lng;
    }
    public int getThick(){
        return this.thick;
}
    public void setThick(int thick){
        this.thick=thick;
    }

}
