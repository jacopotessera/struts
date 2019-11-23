package com.opensymphony.xwork2;

public class BeanBar extends BeanFoo<BeanBar> {
    private String bar;
    public String getBar(){
        return bar;
    }
    public void setBar(String bar){
        this.bar = bar;
    }
}
