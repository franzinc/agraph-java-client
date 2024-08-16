package test.suites;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectPackages("test.openrdf.repository")
public class RepositoryTests {
}
