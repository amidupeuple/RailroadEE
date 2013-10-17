import junit.framework.TestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import javax.ejb.embeddable.EJBContainer;
import javax.naming.Context;
import javax.naming.NamingException;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ScheduleIntegrationTestCase {

    private static EJBContainer ejbContainer;
    private static Context ctx;

    @BeforeClass
    public static void setUpClass() {
        Map properties = new HashMap();
        properties.put(EJBContainer.MODULES, new File("C:\\Users\\danya_000\\IdeaProjects\\RailroadEE\\ejb\\target\\classes"));
        ejbContainer = EJBContainer.createEJBContainer(properties);
        ctx = ejbContainer.getContext();
    }

    @AfterClass
    public static void tearDown() {
        ejbContainer.close();
    }

    @org.junit.Test
    public void testGetHello() throws NamingException {
        ScheduleBean bean = (ScheduleBean) ctx.lookup("java:global/classes/ScheduleEJB");
        String result = bean.getHello();
        TestCase.assertEquals("Hello", result);
    }
}
