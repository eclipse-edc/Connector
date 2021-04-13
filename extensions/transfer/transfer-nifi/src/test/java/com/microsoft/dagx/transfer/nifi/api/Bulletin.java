package com.microsoft.dagx.transfer.nifi.api;

public class Bulletin {
    public int id;
    public String groupId;
    public String sourceId;
    public String timestamp;
    public boolean canRead;
    public BulletinDetails bulletin;
}
