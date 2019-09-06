package com.cva.test;

import java.io.BufferedReader;
import java.io.FileReader;

import java.util.Arrays;
import java.util.Scanner;

public class TestUtils {



    static int[][] ReadArray(String filename) throws Exception{
        Scanner sc = new Scanner(new BufferedReader(new FileReader(filename)));
        int rows = 3;
        int columns = 256;
        int [][] myArray = new int[rows][columns];
        while(sc.hasNextLine()) {
            String row = sc.nextLine();
            for (int i=0; i<myArray.length; i++) {
                String[] line = row.trim().split(" ");
                for (int j=0; j<line.length; j++) {
                    myArray[i][j] = Integer.parseInt(line[j]);
                }
            }
        }

    return myArray;
    }



}
