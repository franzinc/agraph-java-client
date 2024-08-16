package test.suites;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
        test.UnicodeTest.class,
        test.UnicodeRDFFormatTest.class,
        test.UnicodeTQRFormatTest.class
})
public class UnicodeTests {
}
