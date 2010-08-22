package ca.hullabaloo.properties;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Properties;

@Test
public class CglibPropertyImplTest {
    public static class TestCl {
        public String getFoo() {
            return "13";
        }
    }

    public void testDefault() {
        TestCl instance = CglibPropertyImpl.create(Resolvers.viewOver(), TestCl.class);
        Assert.assertEquals(instance.getFoo(), "13");
    }

    public void testWithOverride() {
        Properties p = new Properties();
        p.setProperty("foo", "31");
        TestCl instance = CglibPropertyImpl.create(Resolvers.viewOver(p), TestCl.class);
        Assert.assertEquals(instance.getFoo(), "31");
    }

    @Namespace("tw.bar")
    public static class TestNsCl {
        public String getFoo() {
            return "13";
        }
    }

    public void testNamespaceDefault() {
        TestNsCl instance = CglibPropertyImpl.create(Resolvers.viewOver(), TestNsCl.class);
        Assert.assertEquals(instance.getFoo(), "13");
    }

    public void testNamespaceWithOverride() {
        Properties p = new Properties();
        p.setProperty("foo", "21");
        p.setProperty("tw.bar.foo", "31");
        TestNsCl instance = CglibPropertyImpl.create(Resolvers.viewOver(p), TestNsCl.class);
        Assert.assertEquals(instance.getFoo(), "31");
    }

    public static abstract class TestAbstract {
        public abstract String getFoo();
    }

    public void testAbstractWithOverride() {
        Properties p = new Properties();
        p.setProperty("foo", "13");
        TestAbstract instance = CglibPropertyImpl.create(Resolvers.viewOver(p),TestAbstract.class);
        Assert.assertEquals(instance.getFoo(),"13");
    }

    @Test(expectedExceptions = JavaPropertyException.class)
    public void testAbstractWithoutOverride() {
        CglibPropertyImpl.create(Resolvers.viewOver(), TestAbstract.class);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static abstract class TestBadAbstract {
        abstract String getFoo();
    }

    @Test(expectedExceptions = JavaPropertyException.class)
    public void testAbstractWithNonPublicMethod() {
        CglibPropertyImpl.create(Resolvers.viewOver(), TestBadAbstract.class);
    }
}