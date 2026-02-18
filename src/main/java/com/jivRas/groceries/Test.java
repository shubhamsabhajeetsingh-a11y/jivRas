package com.jivRas.groceries;

import java.util.HashMap;

public class Test {
	
	public static void main(String[] args) {
		HashMap<String, Integer> groceries = new HashMap<>();
		groceries.put("Apples", 5);
		groceries.put("Bananas", 3);
		groceries.put("Oranges", 4);
		
		//sorting the groceries by values and printing
		
		//by comparable interface
		groceries.entrySet()
		         .stream()
		         .sorted((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
		         .forEach(e -> System.out.println(e.getKey() + ": " + e.getValue()));
		
		//by comparator interface
		System.out.println("Using Comparator Interface:");
		groceries.entrySet()
		         .stream()
		         .sorted(java.util.Comparator.comparingInt(e -> e.getValue()))
		         .forEach(e -> System.out.println(e.getKey() + ": " + e.getValue()));
		
		String str ="jahaj";
		
	String rev="";
	int len=str.length();
	for(int i=0; i<len; i++) {
		rev= str.charAt(i)+rev;
	}
	if(str.equals(rev)) {
		System.out.println("palindrome");
	}else {
		System.out.println("not a palindrome");}
	
	
	
	
	
	
		
	}
	
	
	
	
}

