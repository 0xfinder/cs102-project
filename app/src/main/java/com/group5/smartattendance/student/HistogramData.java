package com.group5.smartattendance.student;

public class HistogramData {
    double highestScore;
    String highestScoreID;

    public HistogramData() {
        this.highestScore = 0.0;
        this.highestScoreID = "unknown";
    }

    public double getHighestScore() {
        return this.highestScore;
    }

    public String getHighestScoreID() {
        return this.highestScoreID;
    }

    public void setHighestScore(double score) {
        this.highestScore = score;
    }

    public void setHighestScoreID(String id) {
        this.highestScoreID = id;
    }

}
