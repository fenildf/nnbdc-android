package com.nb.nnbdc.android.util;

import beidanci.vo.UserVo;

public class Msg {
    private String type;
    private String content;
    private UserVo sender;
    private Object[] args;

    public Msg(String type, String content, UserVo sender, Object[] args, boolean viewed) {
        this.type = type;
        this.content = content;
        this.sender = sender;
        this.args = args;
        this.viewed = viewed;
    }

    private boolean viewed;

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

    public boolean isViewed() {
        return viewed;
    }
}
