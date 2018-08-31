package com.developers.meraki.projectw;

public class CompareResult {
    private double similarity;
    private String wellName;

    public void set(String wellName, double similarity){
        this.similarity = similarity;
        this.wellName = wellName;
    }

    public double getSimilarity(){
        return this.similarity;
    }

    public String getWellName(){
        return this.wellName;
    }
}
