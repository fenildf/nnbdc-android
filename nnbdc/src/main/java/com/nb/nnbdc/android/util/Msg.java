package com.nb.nnbdc.android.util;

import java.util.Date;

import beidanci.vo.UserVo;

public class Msg {
    private String type;
    private String content;
    private UserVo sender;
    private Object[] args;

    public Date getReceiveTime() {
        return receiveTime;
    }

    private Date receiveTime;

    public Msg(String type, String content, UserVo sender, Object[] args, boolean hasBeenRead) {
        this.type = type;
        this.content = content;
        this.sender = sender;
        this.args = args;
        this.hasBeenRead = hasBeenRead;
        this.receiveTime = new Date();
    }

    private boolean hasBeenRead;

    public String getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public UserVo getSender() {
        return sender;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setHasBeenRead(boolean hasBeenRead) {
        this.hasBeenRead = hasBeenRead;
    }

    public boolean isHasBeenRead() {
        return hasBeenRead;
    }
}
