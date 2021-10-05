package me.chenzz.mweb.format;

public class TitleInfo {

    private Integer titleNo;
    private Integer titleLevel;

    public TitleInfo(Integer titleNo, Integer titleLevel) {
        this.titleNo = titleNo;
        this.titleLevel = titleLevel;
    }

    public Integer getTitleNo() {
        return titleNo;
    }

    public void setTitleNo(Integer titleNo) {
        this.titleNo = titleNo;
    }

    public Integer getTitleLevel() {
        return titleLevel;
    }

    public void setTitleLevel(Integer titleLevel) {
        this.titleLevel = titleLevel;
    }
}
