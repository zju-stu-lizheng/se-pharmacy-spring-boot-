package zju.se.pharmacy;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Iterator;
import java.util.Vector;

import org.databene.contiperf.PerfTest;
import org.databene.contiperf.junit.ContiPerfRule;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ConcurrentTests {
    @Rule
    public ContiPerfRule i = new ContiPerfRule();

    @Test
    @PerfTest(duration = 10 * 1000)
    public void userVerifyTest() {
        try {
            MyJDBC.connectDatabase();
            // Obtaining an iterator
            Iterator<String> it = MyManager.aid.iterator();
            Iterator<String> pwd = MyManager.passwd.iterator();

            while (it.hasNext()) {
                MyJDBC.ensureLogin(it.next(), pwd.next());
            }

        } catch (Exception ignored) {
        }
    }

    @Test
    @PerfTest(duration = 10 * 1000)
    public void databaseTest() {
        try {
            MyJDBC.connectDatabase();
            MyJDBC.queryMedicine((int) (Math.random() * 10));
        } catch (Exception ignored) {
        }
    }
}