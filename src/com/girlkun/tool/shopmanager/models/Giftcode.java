package com.girlkun.tool.shopmanager.models;

import java.sql.Timestamp;

public class Giftcode {
    public int id;
    public String code;
    public int countLeft;
    public String detail;
    public Timestamp dateCreate;
    public Timestamp expired;
    public int type;

    // Display property
    public String formattedDetail;

    public Giftcode() {
    }
}
