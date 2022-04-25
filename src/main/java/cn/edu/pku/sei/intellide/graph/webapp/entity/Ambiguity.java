package cn.edu.pku.sei.intellide.graph.webapp.entity;

import java.util.ArrayList;

import java.util.List;

public class Ambiguity {
    public String text;
    public String cypher;
    public List<String> mapValues = new ArrayList<>();
    public List<String> mapTypes = new ArrayList<>();
    public List<String> mapLabels = new ArrayList<>();
    
    public Ambiguity() {
		// TODO Auto-generated constructor stub
	}
    
    public Ambiguity(Ambiguity a) {
		// TODO Auto-generated constructor stub
    	this.text = a.text;
    	this.mapLabels = a.mapLabels;
    	this.mapTypes = a.mapTypes;
    	this.mapValues = a.mapValues;
    }
    
    public Ambiguity(String text) {
		// TODO Auto-generated constructor stub
    	this.text = text;
	}
    
    public void addMap(String mapValue,String mapType,String mapLabel) {
    	mapValues.add(mapValue);
    	mapTypes.add(mapType);
    	mapLabels.add(mapLabel);
    }
}
