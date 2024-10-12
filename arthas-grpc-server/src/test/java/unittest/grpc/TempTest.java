package unittest.grpc;

import org.junit.Before;
import org.junit.Test;

/**
 * @author: FengYe
 * @date: 2024/10/13 03:03
 * @description: TempTest
 */
public class TempTest {

    private int num;

    @Before
    public void before() throws InterruptedException {
        System.out.println("before start,"+num++);
        System.out.println(Thread.currentThread().getId());
        Thread.sleep(10000L);
        System.out.println("before end");
    }

    @Test
    public void test1() throws InterruptedException {
        System.out.println("test1 start");
        Thread.sleep(1000L);
        System.out.println("test1 end");
    }

    @Test
    public void test2() throws InterruptedException {
        System.out.println("test2 start");
        Thread.sleep(1000L);
        System.out.println("test2 end");
    }
}
