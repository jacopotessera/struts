package com.opensymphony.xwork2;

public abstract class BeanFoo<T extends BeanFoo> {
    private String foo;
    public String getFoo(){
        return foo;
    }
    public void setFoo(String foo){
        this.foo = foo;
    }
}
