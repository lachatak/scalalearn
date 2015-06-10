package org.kaloz.homework;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ZipTest {

    private Zip obj = new Zip();

    @Test
    public void test() {

        int result = obj.solution(12345, 678);
        assertEquals(16273845, result);
    }

    @Test
    public void test2() {

        int result = obj.solution(0, 678);
        assertEquals(678, result);
    }

    @Test
    public void test3() {

        int result = obj.solution(678, 0);
        assertEquals(6078, result);
    }

    @Test
    public void test4() {

        int result = obj.solution(100000000, 100000000);
        assertEquals(-1, result);
    }


    @Test
    public void test5() {

        int result = obj.solution(0, 0);
        assertEquals(0, result);
    }
}
