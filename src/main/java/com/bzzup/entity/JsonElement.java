package com.bzzup.entity;

public class JsonElement {

	private String key;
	private String value;
	private String type;
	private int size;
	private int childSize;
	
	public static final String TYPE_ELEMENT = "Element";
	public static final String TYPE_NODE = "Node";
	
	public JsonElement(String key, String value) {
		this.key = key;
		this.value = value;
	}
	
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}
