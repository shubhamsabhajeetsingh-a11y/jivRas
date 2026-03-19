package com.jivRas.groceries;

import java.util.ArrayList;
import java.util.List;

public class Test {
	
	public static void main(String[] args) {
		//k=37 convert into binary and put into array and find the number of 1's repeated and after that position of 1into array output:[3,1,4,6]
		
		int k=37;
		String binary=Integer.toBinaryString(k);
		System.out.println("binary"+binary);
		char[] arr=binary.toCharArray();
		//int[] result=new int[arr.length];
		List<Integer> result=new ArrayList<>();
		int count=0;
		for(int i=0;i<arr.length;i++) {
			if(arr[i]=='1') {
				count=count+1;
			}
			
		}
		System.out.println("count"+count);
		result.add(0,count);
	int index=1;
	for(int i=0;i<arr.length;i++) {
		
		if(arr[i]=='1') {
			result.add(index,i);
			index++;
		}
	
	}
	for(int i=0;i<result.size();i++) {
		System.out.print(result+" ");}
	
	}
	
	
	
	
}

