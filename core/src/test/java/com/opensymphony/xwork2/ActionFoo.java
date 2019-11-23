package com.opensymphony.xwork2;

public abstract class ActionFoo<T extends BeanFoo<T>> extends ActionSupport {
	private T item;
	public T getItem(){
		return item;
	}
	public void setItem(T item){
		this.item = item;
	}
}
