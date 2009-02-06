#
#    Licensed to the Apache Software Foundation (ASF) under one or more
#    contributor license agreements.  See the NOTICE file distributed with
#    this work for additional information regarding copyright ownership.
#    The ASF licenses this file to You under the Apache License, Version 2.0
#    (the "License"); you may not use this file except in compliance with
#    the License.  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS,
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#    See the License for the specific language governing permissions and
#    limitations under the License.
#

require "buildr"
require "buildr/openjpa"
require "buildr/javacc"
require "buildr/jetty"
require "buildr/hibernate"
require "tasks/xmlbeans"



# Keep this structure to allow the build system to update version numbers.
VERSION_NUMBER = "2.0-beta1"
NEXT_VERSION = "2.0-beta1"

# finds one or artifacts by a regex in a set of artifacts
def findArtifacts(artifacts, expr)
    artifacts.flatten.reject { |item| item.is_a?(String) ? !item.match(/^#{expr}/) : !item.to_spec.match(/^#{expr}/) }
end

## all axis2 jar that are in their distro-war.
AXIS2_DEPS = [
    "javax.activation:activation:jar:1.1",
    "annogen:annogen:jar:0.1.0",
    "antlr:antlr:jar:2.7.7",

    group("axiom-api", "axiom-impl", "axiom-dom",
          :under=>"org.apache.ws.commons.axiom", :version=>"1.2.7"),

    group("axis2-adb", "axis2-adb-codegen", "axis2-codegen", "axis2-corba", "axis2-fastinfoset", "axis2-java2wsdl", 
          "axis2-jaxbri", "axis2-jaxws", "axis2-jaxws-api", "axis2-jibx", "axis2-json", "axis2-jws-api", "axis2-kernel",
          "axis2-metadata", "axis2-mtompolicy", "axis2-saaj", "axis2-saaj-api", "axis2-spring", "axis2-xmlbeans",
          :under=>"org.apache.axis2", :version=>"1.4.1"),
          
    "backport-util-concurrent:backport-util-concurrent:jar:3.1",

    "commons-codec:commons-codec:jar:1.3",
    "commons-fileupload:commons-fileupload:jar:1.2",
    "commons-httpclient:commons-httpclient:jar:3.1",
    "commons-io:commons-io:jar:1.4",
    "commons-logging:commons-logging:jar:1.1.1",

    "org.apache.geronimo.specs:geronimo-activation_1.1_spec:jar:1.0.1",
    "org.apache.geronimo.specs:geronimo-annotation_1.0_spec:jar:1.1",
    "org.apache.geronimo.specs:geronimo-javamail_1.4_spec:jar:1.2",
    "org.apache.geronimo.specs:geronimo-stax-api_1.0_spec:jar:1.0.1",

    group("httpcore", "httpcore-nio", 
          :under=>"org.apache.httpcomponents", :version=>"4.0-beta1"),

    "javax.xml.bind:jaxb-api:jar:2.1",
    group("jaxb-impl", "jaxb-xjc",
          :under=>"com.sun.xml.bind", :version=>"2.1.6"),

    "jaxen:jaxen:jar:1.1.1",

    "org.codehaus.jettison:jettison:jar:1.0-RC2",

    "org.jibx:jibx-bind:jar:1.1.5",
    "org.jibx:jibx-run:jar:1.1.5",

    "log4j:log4j:jar:1.2.15",

    "org.apache.neethi:neethi:jar:2.0.4",

    group("woden-api", "woden-impl-dom",
          :under=>"org.apache.woden", :version=>"1.0M8"),

    "wsdl4j:wsdl4j:jar:1.6.2",
    "woodstox:wstx-asl:jar:3.2.4",
    "org.apache.ode:xalan:jar:2.7.0-2",
    "xerces:xercesImpl:jar:2.8.1",
    "org.apache.xmlbeans:xmlbeans:jar:2.3.0",
    "org.apache.ws.commons.schema:XmlSchema:jar:1.4.2",
    "xml-apis:xml-apis:jar:1.3.04",
    "xml-resolver:xml-resolver:jar:1.2",
    "javax.mail:mail:jar:1.4"
    #"soapmonitor:jar:1.4.1", # aren't they actually module archives?
]

ACTIVEMQ            = "org.apache.activemq:apache-activemq:jar:4.1.1"
ANNONGEN            = findArtifacts(AXIS2_DEPS, "annogen:annogen") #"annogen:annogen:jar:0.1.0"
ANT                 = "ant:ant:jar:1.6.5" # TODO: do we need ant? Currently referenced in JBI deployable
AXIOM               = findArtifacts(AXIS2_DEPS, "org.apache.ws.commons.axiom") #[ group("axiom-api", "axiom-impl", "axiom-dom", :under=>"org.apache.ws.commons.axiom", :version=>"1.2.7") ]
AXIS2_MODULES        = struct(
 :mods              => ["org.apache.rampart:rampart:mar:1.4", 
                         "org.apache.rampart:rahas:mar:1.4",
                         "org.apache.axis2:addressing:mar:1.4",
                              "org.apache.axis2:mex:mar:1.41"],
 :libs              => [group("rampart-core", "rampart-policy", "rampart-trust",
                              :under=>"org.apache.rampart",
                              :version=>"1.4"), 
                        "org.apache.ws.security:wss4j:jar:1.5.4", 
                        "org.apache.santuario:xmlsec:jar:1.4.1",
                        "opensaml:opensaml:jar:1.1",
                        "bouncycastle:bcprov-jdk15:jar:132"]
)
AXIS2_WAR           = "org.apache.axis2:axis2-webapp:war:1.4.1"
AXIS2_ALL           = AXIS2_DEPS #group("axis2-adb", "axis2-codegen", "axis2-kernel", "axis2-java2wsdl", "axis2-jibx", "axis2-saaj", "axis2-xmlbeans", :under=>"org.apache.axis2", :version=>"1.4.1")
HTTPCORE            = findArtifacts(AXIS2_DEPS, "org.apache.httpcomponents") #group("httpcore", "httpcore-nio", :under=>"org.apache.httpcomponents", :version=>"4.0-beta1")
BACKPORT            = findArtifacts(AXIS2_DEPS, "backport-util-concurrent:backport-util-concurrent") #backport-util-concurrent:backport-util-concurrent:jar:3.1"
COMMONS             = struct(
  :codec            => findArtifacts(AXIS2_DEPS, "commons-codec:commons-codec"), #"commons-codec:commons-codec:jar:1.3",
  :collections      =>"commons-collections:commons-collections:jar:3.1",
  :dbcp             =>"commons-dbcp:commons-dbcp:jar:1.2.1",
  :fileupload       =>findArtifacts(AXIS2_DEPS, "commons-fileupload:commons-fileupload"), #"commons-fileupload:commons-fileupload:jar:1.2",
  :httpclient       =>findArtifacts(AXIS2_DEPS, "commons-httpclient:commons-httpclient"), #"commons-httpclient:commons-httpclient:jar:3.1",
  :lang             =>"commons-lang:commons-lang:jar:2.1",
  :logging          =>findArtifacts(AXIS2_DEPS, "commons-logging:commons-logging"), #"commons-logging:commons-logging:jar:1.1.1",
  :pool             =>"commons-pool:commons-pool:jar:1.2",
  :primitives       =>"commons-primitives:commons-primitives:jar:1.0"
)
DERBY               = "org.apache.derby:derby:jar:10.4.1.3"
DERBY_TOOLS         = "org.apache.derby:derbytools:jar:10.4.1.3"
DOM4J               = "dom4j:dom4j:jar:1.6.1"
GERONIMO            = struct(
  :kernel           =>"org.apache.geronimo.modules:geronimo-kernel:jar:2.0.1",
  :transaction      =>"org.apache.geronimo.components:geronimo-transaction:jar:2.0.1",
  :connector        =>"org.apache.geronimo.components:geronimo-connector:jar:2.0.1"
)
HIBERNATE           = [ "org.hibernate:hibernate:jar:3.2.5.ga", "asm:asm:jar:1.5.3",
                        "antlr:antlr:jar:2.7.6", "cglib:cglib:jar:2.1_3", "net.sf.ehcache:ehcache:jar:1.2.3" ]
HSQLDB              = "hsqldb:hsqldb:jar:1.8.0.7"
JAVAX               = struct(
  :activation       => findArtifacts(AXIS2_DEPS, "org.apache.geronimo.specs:geronimo-activation_1.1_spec"),   #"javax.activation:activation:jar:1.1", # TODO: do we use Sun's or Geronimo's?
  #:activation       =>"geronimo-spec:geronimo-spec-activation:jar:1.0.2-rc4",
  :connector        => "org.apache.geronimo.specs:geronimo-j2ee-connector_1.5_spec:jar:1.0",
  :ejb              => "org.apache.geronimo.specs:geronimo-ejb_2.1_spec:jar:1.1",
  :javamail         => findArtifacts(AXIS2_DEPS, "org.apache.geronimo.specs:geronimo-javamail_1.4_spec"), # "geronimo-spec:geronimo-spec-javamail:jar:1.3.1-rc5",
  :jms              =>"geronimo-spec:geronimo-spec-jms:jar:1.1-rc4",
  :persistence      =>"javax.persistence:persistence-api:jar:1.0",
  :servlet          =>"org.apache.geronimo.specs:geronimo-servlet_2.4_spec:jar:1.0",
  :stream           => findArtifacts(AXIS2_DEPS, "org.apache.geronimo.specs:geronimo-stax-api_1.0_spec"), #"stax:stax-api:jar:1.0.1",
  :transaction      =>"org.apache.geronimo.specs:geronimo-jta_1.1_spec:jar:1.1"
)
JAXEN               = findArtifacts(AXIS2_DEPS, "jaxen:jaxen") #"jaxen:jaxen:jar:1.1.1"
JBI                 = "org.apache.servicemix:servicemix-jbi:jar:3.1.1-incubating"
JENCKS              = "org.jencks:jencks:jar:all:1.3"
JIBX                = findArtifacts(AXIS2_DEPS, "jibx:jibx-run") #"jibx:jibx-run:jar:1.1-beta3"
LOG4J               = findArtifacts(AXIS2_DEPS, "log4j:log4j") #"log4j:log4j:jar:1.2.15"
OPENJPA             = ["org.apache.openjpa:openjpa:jar:1.1.0",
                       "net.sourceforge.serp:serp:jar:1.13.1"]

SAXON               = group("saxon", "saxon-xpath", "saxon-dom", "saxon-xqj", :under=>"net.sf.saxon", :version=>"9.x")
SERVICEMIX          = group("servicemix-core", "servicemix-shared", "servicemix-services",
                        :under=>"org.apache.servicemix", :version=>"3.1-incubating")
SPRING              = group("spring-beans", "spring-context", "spring-core", "spring-jmx",
                        :under=>"org.springframework", :version=>"2.0.1")
TRANQL              = [ "tranql:tranql-connector:jar:1.1", "axion:axion:jar:1.0-M3-dev", COMMONS.primitives ]
WOODSTOX            = findArtifacts(AXIS2_DEPS, "woodstox:wstx-asl") #"woodstox:wstx-asl:jar:3.2.1"
WSDL4J              = findArtifacts(AXIS2_DEPS, "wsdl4j:wsdl4j") #"wsdl4j:wsdl4j:jar:1.6.2"
XALAN               = findArtifacts(AXIS2_DEPS, "org.apache.ode:xalan") #"org.apache.ode:xalan:jar:2.7.0-2"
XERCES              = findArtifacts(AXIS2_DEPS, "xerces:xercesImpl") #"xerces:xercesImpl:jar:2.9.0"
XSTREAM             = "xstream:xstream:jar:1.2"
WS_COMMONS          = struct(
  :axiom            =>AXIOM,
  :neethi           =>findArtifacts(AXIS2_DEPS, "org.apache.neethi:neethi"), #"org.apache.neethi:neethi:jar:2.0.2",
  :xml_schema       =>findArtifacts(AXIS2_DEPS, "org.apache.ws.commons.schema:XmlSchema") #"org.apache.ws.commons.schema:XmlSchema:jar:1.3.2"
)
XBEAN               = group("xbean-classloader", "xbean-kernel", "xbean-server", "xbean-spring",
                        :under=>"org.apache.xbean", :version=>"2.8")
XMLBEANS            = findArtifacts(AXIS2_DEPS, "org.apache.xmlbeans:xmlbeans") #"org.apache.xmlbeans:xmlbeans:jar:2.3.0"
WODEN               = findArtifacts(AXIS2_DEPS, "org.apache.woden") #["org.apache.woden:woden-api:jar:1.0M8", "org.apache.woden:woden-impl-dom:jar:1.0M8"]

repositories.remote << "http://pxe.intalio.org/public/maven2"
repositories.remote << "http://people.apache.org/repo/m2-incubating-repository"
repositories.remote << "http://repo1.maven.org/maven2"
repositories.remote << "http://people.apache.org/repo/m2-snapshot-repository"
repositories.remote << "http://download.java.net/maven/2"
repositories.remote << "http://ws.zones.apache.org/repository2"
repositories.release_to[:url] ||= "sftp://guest@localhost/home/guest"

# Changing releases tag names
class Release
  class << self
    def tag_with_apache_ode(version)
      tag_without_apache_ode("APACHE_ODE_#{version.upcase}")
    end
    #alias :tag_without_apache_ode :tag
    alias :tag :tag_with_apache_ode 
  end
end


desc "Apache ODE"
#define "ode", :group=>"org.apache.ode", :version=>VERSION_NUMBER do
define "ode" do
  project.version = VERSION_NUMBER
  project.group = "org.apache.ode"

  compile.options.source = "1.5"
  compile.options.target = "1.5"
  manifest["Implementation-Vendor"] = "Apache Software Foundation"
  meta_inf << file("NOTICE")

  desc "ODE Axis Integration Layer"
  define "axis2" do
    compile.with projects("bpel-api", "bpel-compiler", "bpel-connector", "bpel-dao", "il-common", "engine",
      "scheduler-simple", "bpel-schemas", "bpel-store", "utils"),
      AXIOM, AXIS2_ALL, COMMONS.lang, COMMONS.logging, COMMONS.collections, COMMONS.httpclient, DERBY, GERONIMO.kernel, GERONIMO.transaction,
      JAVAX.activation, JAVAX.servlet, JAVAX.stream, JAVAX.transaction, JENCKS, WSDL4J, WS_COMMONS.xml_schema,
      XMLBEANS, AXIS2_MODULES.libs

    test.exclude 'org.apache.ode.axis2.management.*'
    test.with project("tools"), AXIOM, JAVAX.javamail, COMMONS.codec, COMMONS.httpclient, XERCES, WOODSTOX

    package :jar
  end

  desc "ODE Axis2 Based Web Application"
  define "axis2-war" do
    libs = projects("axis2", "bpel-api", "bpel-compiler", "bpel-connector", "bpel-dao",
      "il-common", "runtimes", "bpel-ql", "engine", "scheduler-simple",
      "bpel-schemas", "bpel-store", "dao-hibernate", "jacob", "jca-ra", "jca-server",
      "utils", "dao-jpa"),
      
      AXIS2_ALL, ANNONGEN, BACKPORT, COMMONS.codec, COMMONS.collections, COMMONS.fileupload, COMMONS.httpclient,
      COMMONS.lang, COMMONS.logging, COMMONS.pool, DERBY, DERBY_TOOLS, JAXEN, JAVAX.activation, JAVAX.ejb, JAVAX.javamail,
      JAVAX.connector, JAVAX.jms, JAVAX.persistence, JAVAX.transaction, JAVAX.stream,  JIBX,
      GERONIMO.connector, GERONIMO.kernel, GERONIMO.transaction, LOG4J, OPENJPA, SAXON, TRANQL, WODEN,
      WOODSTOX, WSDL4J, WS_COMMONS.axiom, WS_COMMONS.neethi, WS_COMMONS.xml_schema, XALAN, XERCES, XMLBEANS, AXIS2_MODULES.libs

    package(:war).with(:libs=>libs).path("WEB-INF").tap do |web_inf|
      web_inf.merge project("dao-jpa-db").package(:zip)
      web_inf.merge project("dao-hibernate-db").package(:zip)
      web_inf.include project("axis2").path_to("src/main/wsdl/*")
      web_inf.include project("bpel-schemas").path_to("src/main/xsd/pmapi.xsd")
    end
    package(:war).path("WEB-INF/modules").include(artifacts(AXIS2_MODULES.mods))
    package(:war).tap do |root|
      root.merge(artifact(AXIS2_WAR)).exclude("WEB-INF/**/*").exclude("META-INF/**/*")
    end

    task("start"=>[package(:war), jetty.use]) do |task|
      class << task ; attr_accessor :url, :path ; end
      task.url = "http://localhost:8080/ode"
      task.path = jetty.deploy(task.url, task.prerequisites.first)
      jetty.teardown task("stop")
    end

    task("stop") do |task|
      if url = task("start").url rescue nil
        jetty.undeploy url
      end
    end
    
    test.using :testng
    test.with(projects("tools"), libs, AXIS2_MODULES.mods, AXIS2_ALL, HTTPCORE, JAVAX.servlet, Buildr::Jetty::REQUIRES, file(_("target/test"))).using(:fork => :each)
    test.setup task(:prepare_webapp) do |task|
      cp_r _("src/main/webapp"), _("target/test")
      cp Dir[_("src/main/webapp/WEB-INF/classes/*")], _("target/test")
      cp Dir[project("axis2").path_to("src/main/wsdl/*")], _("target/test/webapp/WEB-INF")
      cp project("bpel-schemas").path_to("src/main/xsd/pmapi.xsd"), _("target/test/webapp/WEB-INF")
      mkdir_p _("target/test/webapp/WEB-INF/processes")
      rm_rf Dir[_("target/test/webapp") + "/**/.svn"]
      mkdir _("target/test/webapp/WEB-INF/processes") unless File.exist?(_("target/test/webapp/WEB-INF/processes"))
      mkdir _("target/test/webapp/WEB-INF/modules") unless File.exist?(_("target/test/webapp/WEB-INF/modules"))
      # move around some property files for test purpose
      mv Dir[_("target/test-classes/TestEndpointProperties/*_global_conf*.endpoint")], _("target/test/webapp/WEB-INF/conf")
      artifacts(AXIS2_MODULES.mods).map {|a| a.invoke }
      cp AXIS2_MODULES.mods.map {|a| repositories.locate(a)} , _("target/test/webapp/WEB-INF/modules")
    end
    test.setup unzip(_("target/test/webapp/WEB-INF")=>project("dao-jpa-db").package(:zip))
    test.setup unzip(_("target/test/webapp/WEB-INF")=>project("dao-hibernate-db").package(:zip))
    test.exclude('*') unless Buildr.environment != 'test'

    test.setup prepare_secured_services_tests(_("target/test/resources/TestRampartBasic/secured-services"), "sample*.axis2")
    test.setup prepare_secured_services_tests(_("target/test/resources/TestRampartPolicy/secured-services"), "sample*-policy.xml")
 
    test.setup prepare_secured_processes_tests(_("target/test/resources/TestRampartBasic/secured-processes"))
    test.setup prepare_secured_processes_tests(_("target/test/resources/TestRampartPolicy/secured-processes"))

  end

  desc "ODE APIs"
  define "bpel-api" do
    compile.with projects("utils", "bpel-schemas"), WSDL4J, COMMONS.logging
    package :jar
  end

  desc "ODE JCA connector"
  define "bpel-api-jca" do
    compile.with project("bpel-api"), JAVAX.connector
    package :jar
  end

  desc "ODE BPEL Compiler"
  define "bpel-compiler" do
    compile.with projects("bpel-api", "runtimes", "bpel-schemas", "utils"),
      COMMONS.logging, COMMONS.collections, JAVAX.stream, JAXEN, SAXON, WSDL4J, XALAN, XERCES
    test.resources { filter(project("bpel-scripts").path_to("src/main/resources")).into(test.resources.target).run }
    package :jar
  end

  desc "ODE JCA Connector Implementation"
  define "bpel-connector" do
    compile.with projects("bpel-api", "bpel-api-jca", "engine", "jca-ra", "jca-server")
    package :jar
  end

  desc "ODE DAO Interfaces"
  define "bpel-dao" do
    compile.with project("bpel-api")
    package :jar
  end

  desc "ODE Interface Layers Common"
  define "il-common" do
    compile.with projects("utils", "bpel-dao", "bpel-api"),
      AXIOM, COMMONS.lang, COMMONS.logging, DERBY, JAVAX.connector, JAVAX.stream, JAVAX.transaction, GERONIMO.transaction, GERONIMO.connector, TRANQL, XMLBEANS
    package :jar
  end

  desc "ODE Runtime repository"
  define "runtimes" do
    compile.from apt
    compile.with projects("bpel-api", "bpel-dao", "jacob", "jacob-ap", "utils"),
      COMMONS.logging, COMMONS.collections, COMMONS.httpclient, COMMONS.lang, JAXEN, JAVAX.persistence,
      JAVAX.stream, SAXON, WSDL4J, XMLBEANS, XERCES, JAVAX.transaction

    package :jar
  end

  desc "ODE BPEL Query Language"
  define "bpel-ql" do
    pkg_name = "org.apache.ode.ql.jcc"
    jjtree = jjtree(_("src/main/jjtree"), :in_package=>pkg_name)
    compile.from javacc(jjtree, :in_package=>pkg_name), jjtree
    compile.with projects("bpel-api", "bpel-compiler", "runtimes", "jacob", "utils")
    package :jar
  end

  desc "ODE Runtime Engine"
  define "engine" do
    compile.with projects("bpel-api", "bpel-compiler", "bpel-dao", "bpel-schemas",
      "bpel-store", "jacob", "jacob-ap", "utils", "il-common"),
      BACKPORT, COMMONS.logging, COMMONS.httpclient, COMMONS.collections, COMMONS.lang, JAXEN, JAVAX.persistence, 
      JAVAX.stream, SAXON, WSDL4J, XMLBEANS, JAVAX.transaction

    test.with projects("scheduler-simple", "dao-jpa", "dao-hibernate", "il-common", "runtimes"),
        BACKPORT, COMMONS.pool, COMMONS.lang, DERBY, JAVAX.connector, JAVAX.transaction,
        GERONIMO.transaction, GERONIMO.kernel, GERONIMO.connector, TRANQL, HSQLDB, JAVAX.ejb,
        LOG4J, XERCES, Buildr::OpenJPA::REQUIRES, XALAN

    package :jar
  end

  desc "ODE Simple Scheduler"
  define "scheduler-simple" do
    compile.with projects("bpel-api", "utils"), COMMONS.collections, COMMONS.logging, JAVAX.transaction
	test.compile.with HSQLDB, GERONIMO.kernel, GERONIMO.transaction
	test.with HSQLDB, JAVAX.transaction, JAVAX.connector, LOG4J,
          GERONIMO.kernel, GERONIMO.transaction, BACKPORT, JAVAX.ejb
    package :jar
  end

  desc "ODE Schemas"
  define "bpel-schemas" do
    compile_xml_beans _("src/main/xsd/*.xsdconfig"), _("src/main/xsd")
    package :jar
  end

  desc "ODE BPEL Test Script Files"
  define "bpel-scripts" do
    package :jar
  end

  desc "ODE Process Store"
  define "bpel-store" do
    compile.with projects("bpel-api", "runtimes", "bpel-compiler", "bpel-dao", "bpel-schemas", "il-common",
      "dao-hibernate", "utils"),
      COMMONS.logging, JAVAX.persistence, JAVAX.stream, HIBERNATE, HSQLDB, XMLBEANS, XERCES, WSDL4J
    compile { open_jpa_enhance }
    resources hibernate_doclet(:package=>"org.apache.ode.store.hib", :excludedtags=>"@version,@author,@todo")

    test.with COMMONS.collections, COMMONS.lang, JAVAX.connector, JAVAX.transaction, DOM4J, LOG4J,
      XERCES, XALAN, JAXEN, SAXON, OPENJPA
    package :jar
  end

  desc "ODE BPEL Tests"
  define "bpel-test" do
    compile.with projects("bpel-api", "bpel-compiler", "bpel-dao", "engine",
      "bpel-store", "utils", "il-common", "dao-jpa", "runtimes"),
      DERBY, JUnit.dependencies, JAVAX.persistence, OPENJPA, WSDL4J, JAVAX.transaction, 
      COMMONS.lang, COMMONS.httpclient, COMMONS.codec

    test.with projects("jacob", "bpel-schemas", "bpel-scripts", "scheduler-simple", "runtimes"),
      COMMONS.collections, COMMONS.lang, COMMONS.logging, DERBY, JAVAX.connector,
      JAVAX.stream, JAVAX.transaction, JAXEN, HSQLDB, LOG4J, SAXON, XERCES, XMLBEANS, XALAN

    package :jar
  end

  desc "ODE Hibernate DAO Implementation"
  define "dao-hibernate" do
    compile.with projects("bpel-api", "bpel-dao", "bpel-ql", "utils"),
      COMMONS.lang, COMMONS.logging, JAVAX.transaction, HIBERNATE, DOM4J
    resources hibernate_doclet(:package=>"org.apache.ode.daohib.bpel.hobj", :excludedtags=>"@version,@author,@todo")

    test.exclude "org.apache.ode.daohib.bpel.BaseTestDAO"
    test.with project("il-common"), BACKPORT, COMMONS.collections, COMMONS.lang, HSQLDB,
      GERONIMO.transaction, GERONIMO.kernel, GERONIMO.connector, JAVAX.connector, JAVAX.ejb, SPRING
    package :jar
  end

  desc "ODE Hibernate Compatible Databases"
  define "dao-hibernate-db" do
    predefined_for = lambda { |name| _("src/main/sql/simplesched-#{name}.sql") }
    properties_for = lambda { |name| _("src/main/sql/ode.#{name}.properties") }

    dao_hibernate = project("dao-hibernate").compile.target
    bpel_store = project("bpel-store").compile.target

    Buildr::Hibernate::REQUIRES[:xdoclet] =  Buildr.group("xdoclet", "xdoclet-xdoclet-module", "xdoclet-hibernate-module",
      :under=>"xdoclet", :version=>"1.2.3") + ["xdoclet:xjavadoc:jar:1.1-j5"]
    export = lambda do |properties, source, target|
      file(target=>[properties, source]) do |task|
        mkpath File.dirname(target), :verbose=>false
        # Protection against a buildr bug until the fix is released, avoids build failure
        class << task ; attr_accessor :ant ; end
        task.enhance { |task| task.ant = Buildr::Hibernate.schemaexport }
        
        hibernate_schemaexport target do |task, ant|
          ant.schemaexport(:properties=>properties.to_s, :quiet=>"yes", :text=>"yes", :delimiter=>";",
                           :drop=>"no", :create=>"yes", :output=>target) do
            ant.fileset(:dir=>source.to_s) { ant.include :name=>"**/*.hbm.xml" }
                           end
        end
      end
    end

    runtime_sql = export[ properties_for[:derby], dao_hibernate, _("target/runtime.sql") ]
    store_sql = export[ properties_for[:derby], bpel_store, _("target/store.sql") ]
    derby_sql = concat(_("target/derby.sql")=>[ predefined_for[:derby], runtime_sql, store_sql ])
    derby_db = Derby.create(_("target/derby/hibdb")=>derby_sql)
    build derby_db

    %w{ mysql firebird hsql postgres sqlserver oracle }.each do |db|
      partial = export[ properties_for[db], dao_hibernate, _("target/partial.#{db}.sql") ]
      build concat(_("target/#{db}.sql")=>[ predefined_for[db], partial ])
    end

    package(:zip).include(derby_db)
  end

  desc "ODE OpenJPA DAO Implementation"
  define "dao-jpa" do
    compile.with projects("bpel-api", "bpel-dao", "utils"),
      COMMONS.collections, COMMONS.logging, JAVAX.connector, JAVAX.persistence, JAVAX.transaction,
      OPENJPA, XERCES
    compile { open_jpa_enhance }
    package :jar
  end

  desc "ODE OpenJPA Derby Database"
  define "dao-jpa-db" do
    %w{ derby mysql oracle }.each do |db|
      db_xml = _("src/main/descriptors/persistence.#{db}.xml")
      scheduler_sql = _("src/main/scripts/simplesched-#{db}.sql")
      partial_sql = file("target/partial.#{db}.sql"=>db_xml) do |task|
        mkpath _("target"), :verbose=>false
        Buildr::OpenJPA.mapping_tool :properties=>db_xml, :action=>"build", :sql=>task.name,
          :classpath=>projects("bpel-store", "dao-jpa", "bpel-api", "bpel-dao", "utils" )
      end
      sql = concat(_("target/#{db}.sql")=>[_("src/main/scripts/license-header.sql"), partial_sql, scheduler_sql])
      build sql
    end
    derby_db = Derby.create(_("target/derby/jpadb")=>_("target/derby.sql"))

    test.with projects("bpel-api", "bpel-dao", "runtimes", "il-common", "dao-jpa", "utils"),
      BACKPORT, COMMONS.collections, COMMONS.lang, COMMONS.logging, GERONIMO.transaction,
      GERONIMO.kernel, GERONIMO.connector, HSQLDB, JAVAX.connector, JAVAX.ejb, JAVAX.persistence,
      JAVAX.transaction, LOG4J, OPENJPA, XERCES, WSDL4J

    build derby_db
    package(:zip).include(derby_db)
  end

  desc "ODE JAva Concurrent OBjects"
  define "jacob" do
    compile.with projects("utils", "jacob-ap"), COMMONS.logging
    compile.from apt

    package :jar
  end

  desc "ODE Jacob APR Code Generation"
  define "jacob-ap" do
    package :jar
  end

  desc "ODE JBI Integration Layer"
  define "jbi" do
    compile.with projects("bpel-api", "bpel-compiler", "bpel-connector", "bpel-dao", "il-common",
      "engine", "scheduler-simple", "bpel-schemas", "bpel-store", "utils", "runtimes"),
      AXIOM, COMMONS.logging, COMMONS.pool, JAVAX.transaction, JBI, LOG4J, WSDL4J, XERCES

    package(:jar)
    package(:jbi).tap do |jbi|
      libs = artifacts(package(:jar),
        projects("bpel-api", "bpel-api-jca", "bpel-compiler", "bpel-connector", "bpel-dao",
        "il-common", "jca-ra", "jca-server", "runtimes", "bpel-ql", "engine",
        "scheduler-simple", "bpel-schemas", "bpel-store", "dao-hibernate", "dao-jpa",
        "jacob", "jacob-ap", "utils"),
        ANT, AXIOM, BACKPORT, COMMONS.codec, COMMONS.collections, COMMONS.dbcp, COMMONS.lang, COMMONS.pool,
        COMMONS.primitives, JAXEN, JAVAX.connector, JAVAX.ejb, JAVAX.jms,
        JAVAX.persistence, JAVAX.stream, JAVAX.transaction, LOG4J, OPENJPA, SAXON, TRANQL,
        XALAN, XMLBEANS, XSTREAM, WSDL4J)

      jbi.component :type=>:service_engine, :name=>"OdeBpelEngine", :description=>self.comment
      jbi.component :class_name=>"org.apache.ode.jbi.OdeComponent", :delegation=>:self, :libs=>libs
      jbi.bootstrap :class_name=>"org.apache.ode.jbi.OdeBootstrap", :libs=>libs
      jbi.merge project("dao-hibernate-db").package(:zip)
      jbi.merge project("dao-jpa-db").package(:zip)
      jbi.include path_to("src/main/jbi/ode-jbi.properties")
    end

    test.with projects("dao-jpa", "bpel-compiler", "bpel-api-jca", "jca-ra",
      "jca-server", "jacob"),
      BACKPORT, COMMONS.lang, COMMONS.collections, DERBY, GERONIMO.connector, GERONIMO.kernel,
      GERONIMO.transaction, JAVAX.connector, JAVAX.ejb, JAVAX.persistence, JAVAX.stream,
      JAVAX.transaction, JAXEN, JBI, OPENJPA, SAXON, SERVICEMIX, SPRING, TRANQL,
      XALAN, XBEAN, XMLBEANS, XSTREAM
    test.using :properties=>{ "jbi.install"=>_("target/smixInstallDir"),  "jbi.examples"=>_("../distro/src/examples-jbi/") }
    test.setup unzip(_("target/smixInstallDir/install/ODE")=>project("dao-jpa-db").package(:zip))
  end

  desc "ODE JCA Resource Archive"
  define "jca-ra" do
    compile.with project("utils"), JAVAX.connector
    package :jar
  end

  desc "ODE JCA Server"
  define "jca-server" do
    compile.with projects("jca-ra", "utils"), COMMONS.logging
    package :jar
  end

  desc "ODE Tools"
  define "tools" do
    compile.with projects("bpel-compiler", "utils"), ANT, COMMONS.httpclient, COMMONS.logging
    package :jar
  end

  desc "ODE Utils"
  define "utils" do
    compile.with AXIOM, AXIS2_ALL, COMMONS.collections, COMMONS.logging, COMMONS.pool, COMMONS.httpclient, COMMONS.codec, LOG4J, XERCES, JAVAX.stream, WSDL4J
	test.exclude "*TestResources"
    package :jar
  end

end

define "ode-extensions", :base_dir => "extensions" do
  [:version, :manifest, :meta_inf].each { |prop| send "#{prop}=", project("ode").send(prop) }
  project.group = "org.apache.ode.extensions"

  desc "E4X Extension"
  define "e4x", :version=>"1.0-beta" do
    compile.with "rhino:js:jar:1.7R1", COMMONS.logging, projects("ode:bpel-api", "ode:runtimes", "ode:engine", 
                 "ode:bpel-compiler", "ode:utils")
    test.with "rhino:js:jar:1.7R1", projects("ode:bpel-api", "ode:runtimes", "ode:jacob", "ode:bpel-schemas",
              "ode:bpel-scripts", "ode:scheduler-simple", "ode:bpel-test", "ode:utils", "ode:bpel-compiler",
              "ode:bpel-dao", "ode:engine", "ode:bpel-store", "ode:il-common", "ode:dao-jpa"),
              COMMONS.collections, COMMONS.lang, COMMONS.logging, DERBY, JAVAX.connector,
              JAVAX.stream, JAVAX.transaction, JAXEN, HSQLDB, LOG4J, SAXON, XERCES, XMLBEANS, XALAN,
              DERBY, JUnit.dependencies, JAVAX.persistence, OPENJPA, WSDL4J, JAVAX.transaction
    package :jar
  end

  desc "JMS BPEL event publisher"
  define "jms-eventpublisher", :version=>"1.0-beta" do
    compile.with ACTIVEMQ, projects("ode:bpel-schemas", "ode:bpel-api"), XMLBEANS
    package :jar
  end
end

define "apache-ode" do
  [:version, :group, :manifest, :meta_inf].each { |prop| send "#{prop}=", project("ode").send(prop) }

  def distro(project, postfix)
    id = project.parent.id + postfix
    project.package(:zip, :id=>id).path("#{id}-#{version}").tap do |zip|
      zip.include meta_inf + ["RELEASE_NOTES", "README"].map { |f| path_to(f) }
      zip.path("examples").include project.path_to("src/examples"+postfix), :as=>"."

      # Libraries
      zip.path("lib").include artifacts(COMMONS.logging, COMMONS.codec, COMMONS.httpclient,
        COMMONS.pool, COMMONS.collections, JAXEN, SAXON, LOG4J, WSDL4J, XALAN, XERCES)
      project("ode").projects("utils", "tools", "bpel-compiler", "bpel-api", "runtimes", "bpel-schemas").
        map(&:packages).flatten.each do |pkg|
        zip.include(pkg.to_s, :as=>"#{pkg.id}.#{pkg.type}", :path=>"lib")
      end

      #Include extensions
	  #(create a extensions folder, put extension folders in there, add README per extension if available, 
	  # add README.extensions to extensions folder)
      project("ode-extensions").projects.each do |p|
        p.packages.flatten.select{|pkg| pkg.type == :jar && !pkg.classifier}.each do |art|
          zip.include(art, :path=>"extensions/#{art.id}")
		  if File.exist?(p.path_to("README"))
		    zip.path("extensions/#{art.id}").include p.path_to("README")
		  end
          p.compile.classpath.select{|pkg| pkg.group != project("ode").group}.each do |lib|
            zip.include(lib, :path=>"extensions/#{art.id}/lib")
          end
        end
      end
	  zip.path("extensions").include project("ode-extensions").path_to("README.extensions")

      # Including third party licenses
      Dir["#{project.path_to("license")}/*LICENSE"].each { |l| zip.include(l, :path=>"lib") }
      zip.include(project.path_to("target/LICENSE"))

      # Include supported database schemas
      Dir["#{project("ode:dao-jpa-db").path_to("target")}/*.sql"].each do |f|
        zip.include(f, :path=>"sql") unless f =~ /partial/
      end

      # Tools scripts (like bpelc and sendsoap)
      bins = file(project.path_to("target/bin")=>FileList[project.path_to("src/bin/*")]) do |task|
        mkpath task.name
        cp task.prerequisites, task.name
        chmod 0755, FileList[task.name + "/*"], :verbose=>false
      end
      zip.include(bins)

      yield zip
      # For some reason this always fails on a clean build, commenting until I have time to inquire
      # project.check zip, "should contain mysql.sql" do
      #   it.should contain("sql/mysql.sql")
      # end
    end
  end

  desc "ODE Axis2 Based Distribution"
  define "distro" do
    parent.distro(self, "-war") { |zip| zip.include project("ode:axis2-war").package(:war), :as=>"ode.war" }
    parent.distro(self, "-jbi") { |zip| zip.include project("ode:jbi").package(:zip) }

    # Preparing third party licenses
    build do
      Dir.mkdir(project.path_to("target")) unless File.exist?(project.path_to("target"))
      cp parent.path_to("LICENSE"), project.path_to("target/LICENSE")
      File.open(project.path_to("target/LICENSE"), "a+") do |l|
        l <<  Dir["#{project.path_to("license")}/*LICENSE"].map { |f| "lib/"+f[/[^\/]*$/] }.join("\n")
      end
    end

    project("ode:axis2-war").task("start").enhance do |task|
      target = "#{task.path}/webapp/WEB-INF/processes"
      puts "Deploying processes to #{target}" if verbose
      verbose(false) do
        mkpath target
        cp_r FileList[_("src/examples/*")].to_a, target
        rm Dir.glob("#{target}/*.deployed")
      end
    end
  end

  package(:zip, :id=>"#{id}-sources").path("#{id}-sources-#{version}").tap do |zip|
    if File.exist?(".svn")
      `svn status -v`.reject { |l| l[0] == ?? || l[0] == ?D }.
        map { |l| l.split.last }.reject { |f| File.directory?(f) }.
        each { |f| zip.include f, :as=>f }
    else
      zip.include Dir.pwd, :as=>"."
    end
  end

  package(:zip, :id=>"#{id}-docs").include(javadoc(project("ode").projects).target) unless ENV["JAVADOC"] =~ /^(no|off|false|skip)$/i
end


# Helper methods 
###################
def prepare_secured_processes_tests(test_dir)
  task(test_dir.to_sym) do
    mkdir "#{test_dir}/modules" unless File.directory? "#{test_dir}/modules"
    artifacts(AXIS2_MODULES.mods).map {|a| a.invoke }
    cp AXIS2_MODULES.mods.map {|a| repositories.locate(a)} , _("#{test_dir}/modules")

    Dir.chdir(test_dir) do
      Dir['sample*-service.xml'].each do |service_file|
        sample_name = service_file.split('-').first
        proc_dir = "process-#{sample_name}"
        cp_r "process-template/.", proc_dir
        cp service_file, "#{proc_dir}/HelloService.axis2"
      end
    end
  end
end
def prepare_secured_services_tests(test_dir, file_pattern)
    task(test_dir.to_sym) do 
      # copy the required modules
      mkdir "#{test_dir}/modules" unless File.directory? "#{test_dir}/modules"
      artifacts(AXIS2_MODULES.mods).map {|a| a.invoke }
      cp AXIS2_MODULES.mods.map {|a| repositories.locate(a)} , _("#{test_dir}/modules")
      # generate one process per test
      Dir.chdir(test_dir) do
        Dir[file_pattern].each do |config_file| 
          sample_name = File.basename(config_file, "."+config_file.split('.').last)
          # create process directory
         proc_dir = "process-#{sample_name}"
          mkdir proc_dir unless File.directory? proc_dir
          # copy files
          cp config_file, proc_dir 
          # copy files from template and replace variable names
          Dir["process-template/*"].each do |file|
            lines = IO.readlines(file)
            # copy file and replace template values
            File.open("#{proc_dir}/#{File.basename(file)}", 'w') { |f| 
              lines.each { |l| 
                l.gsub!("{sample.namespace}", "http://#{sample_name.gsub('-','.')}.samples.rampart.apache.org")
                l.gsub!("{sample.service.name}", sample_name)
                f<<l
              }
            }
          end
        end
      end
   end
end
