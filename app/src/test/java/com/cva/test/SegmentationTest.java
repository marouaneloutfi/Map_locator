package com.cva.test;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class SegmentationTest {

    private Segmentation seg = new Segmentation();

    @Test
    public void transposeTest(){
        int[][] original = new int[][]{{1,2,3}, {4,5,6}, {7,8,9}, {10,11,12}};
        int[][] expected = new int[][]{{1,4,7,10}, {2,5,8,11}, {3,6,9,12}};
        assertTrue(Arrays.deepEquals(expected,seg.transpose(original)));

    }

/*
    @Test
    public void pascalColorMapTest(){

        int[][] expected = new int[0][];
        try {
            expected = TestUtils.ReadArray("/home/marouane/pascal_array.txt");
        } catch (Exception e) {
            e.printStackTrace();
        }

        assertTrue(Arrays.deepEquals(expected,seg.PascalColorMap()));


    }
    */
}
