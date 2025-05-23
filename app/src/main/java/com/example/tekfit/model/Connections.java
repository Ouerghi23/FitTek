package com.example.tekfit.model;

public class Connections
{
    public String connectionStatus;

    public Connections()
    {

    }

    public Connections(String connectionStatus)
    {
        this.connectionStatus = connectionStatus;
    }

    public String getConnectionStatus()
    {
        return connectionStatus;
    }

    public void setConnectionStatus(String connectionStatus)
    {
        this.connectionStatus = connectionStatus;
    }
}
