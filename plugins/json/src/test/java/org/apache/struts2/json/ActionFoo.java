package org.apache.struts2.json;

public abstract class ActionFoo<T extends BeanFoo<T>> {
	protected T item;
	public T getItem(){
		return item;
	}
	public void setItem(T item){
		this.item = item;
	}
}
