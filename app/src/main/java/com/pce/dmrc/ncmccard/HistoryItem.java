package com.pce.dmrc.ncmccard;

public class HistoryItem {

    private final String type;
    private final String station;
    private final String fare;
    private final String balance;
    private final String dateTime;

    public HistoryItem(
            String type,
            String station,
            String fare,
            String balance,
            String dateTime
    ) {
        this.type = type;
        this.station = station;
        this.fare = fare;
        this.balance = balance;
        this.dateTime = dateTime;
    }

    public String getType() {
        return type;
    }

    public String getStation() {
        return station;
    }

    public String getFare() {
        return fare;
    }

    public String getBalance() {
        return balance;
    }

    public String getDateTime() {
        return dateTime;
    }
}