import os.path
import re
import subprocess

# This script is used to create a project that will run tests using a staged
# version of the Java client. It creates a POM file the following properties:
#   - It has a dependency on agraph-java-client in the current version,
#     determined using version.sh
#   - OSSRH staging repository is used.
#   - All test scoped dependencies are copied from the main project.
#     This is required because of https://issues.apache.org/jira/browse/MNG-1378
#   - mvn test will run tests from agraph-java-client.
# The POM file will be printed to standard output.

base_dir = os.path.dirname(os.path.realpath(__file__))

dep_pattern = re.compile(r'^\[INFO\]\s*'
                         r'(?P<group>[^:]*):'
                         r'(?P<artifact>[^:]*):'
                         r'(?P<type>[^:]*):'
                         r'(?:(?P<classifier>[^:]*):)?'  # Classifier is optional...
                         r'(?P<version>[^:]*):'
                         r'test\s*$')

dep_template = '''
    <dependency>
      <groupId>{group}</groupId>
      <artifactId>{artifact}</artifactId>
      <version>{version}</version>
      <type>{type}</type>
      <classifier>{classifier}</classifier>
    </dependency>'''

pom_template='''<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.franz</groupId>
  <artifactId>release-test</artifactId>
  <packaging>jar</packaging>
  <version>{version}</version>
  <name>release-test</name>

  <repositories>
    <repository>
      <id>ossrh-staging</id>
      <url>https://oss.sonatype.org/content/groups/staging</url>
    </repository>
  </repositories>

  <properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>2.19.1</version>
        <configuration>
          <dependenciesToScan>
            <dependency>com.franz:agraph-java-client</dependency>
          </dependenciesToScan>
        </configuration>
      </plugin>
    </plugins>
  </build>

  <dependencies>
    <dependency>
      <groupId>com.franz</groupId>
      <artifactId>agraph-java-client</artifactId>
      <version>{version}</version>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>com.franz</groupId>
      <artifactId>agraph-java-client</artifactId>
      <version>{version}</version>
      <type>test-jar</type>
      <scope>test</scope>
    </dependency>
    <!-- Test dependencies are not transitive.
         See https://issues.apache.org/jira/browse/MNG-1378 -->
    {dependencies}
  </dependencies>
</project>'''

version, _ = subprocess.Popen([os.path.join(base_dir, '../version.sh')], 
                              shell=True, stdout=subprocess.PIPE).communicate()
version = str(version,'utf-8').strip()

mvn = subprocess.Popen('mvn dependency:list',
                       cwd=os.path.join(base_dir, '..'),
                       stdout=subprocess.PIPE, 
                       shell=True)

dependencies = []
for line in mvn.stdout.readlines():
    line = str(line,'utf-8');
    m = dep_pattern.match(line)
    if m:
        decl = dep_template.format(**m.groupdict(default=''))
        dependencies.append(decl)

print (pom_template.format(version=version, dependencies='\n'.join(dependencies)))
