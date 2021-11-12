package si.isystem.connect.data;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JVersionTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testCompareTo() {
        JVersion ver = new JVersion(9, 10, 100);
        
        Assert.assertEquals(0, ver.compareTo(new JVersion(9, 10, 100)));
        Assert.assertEquals(1, ver.compareTo(new JVersion(8, 10, 100)));
        Assert.assertEquals(1, ver.compareTo(new JVersion(9, 9, 100)));
        Assert.assertEquals(1, ver.compareTo(new JVersion(9, 10, 99)));

        Assert.assertEquals(-1, ver.compareTo(new JVersion(10, 10, 100)));
        Assert.assertEquals(-1, ver.compareTo(new JVersion(9, 11, 100)));
        Assert.assertEquals(-1, ver.compareTo(new JVersion(9, 10, 101)));
    }
}
