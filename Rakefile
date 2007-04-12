require "buildr/lib/buildr.rb"


# Keep this structure to allow the build system to update version numbers.
VERSION_NUMBER = "2.0-SNAPSHOT"
NEXT_VERSION = "2.1"

ANNONGEN            = "annogen:annogen:jar:0.1.0"
ANT                 = "ant:ant:jar:1.6.5"
AXIOM               = group("axiom-api", "axiom-impl", "axiom-dom", :under=>"org.apache.ws.commons.axiom", :version=>"1.2")
AXIS2               = "org.apache.axis2:axis2:jar:1.1"
AXIS2_ALL           = group("axis2", "axis2-adb", "axis2-codegen", "axis2-tools", "axis2-kernel",
                        "axis2-java2wsdl", "axis2-jibx", "axis2-kernel", "axis2-saaj", "axis2-xmlbeans",
                        :under=>"org.apache.axis2", :version=>"1.1")
BACKPORT            = "backport-util-concurrent:backport-util-concurrent:jar:3.0"
COMMONS             = OpenStruct.new(
  :codec            =>"commons-codec:commons-codec:jar:1.3",
  :collections      =>"commons-collections:commons-collections:jar:3.1",
  :dbcp             =>"commons-dbcp:commons-dbcp:jar:1.2.1",
  :fileupload       =>"commons-fileupload:commons-fileupload:jar:1.0",
  :httpclient       =>"commons-httpclient:commons-httpclient:jar:3.0",
  :lang             =>"commons-lang:commons-lang:jar:2.1",
  :logging          =>"commons-logging:commons-logging:jar:1.0.3",
  :pool             =>"commons-pool:commons-pool:jar:1.2",
  :primitives       =>"commons-primitives:commons-primitives:jar:1.0"
)
DERBY               = "org.apache.derby:derby:jar:10.1.2.1"
DERBY_TOOLS         = "org.apache.derby:derbytools:jar:10.1.2.1"
DOM4J               = "dom4j:dom4j:jar:1.6.1"
GERONIMO            = OpenStruct.new(
  :kernel           =>"org.apache.geronimo.modules:geronimo-kernel:jar:1.2-beta",
  :transaction      =>"org.apache.geronimo.modules:geronimo-transaction:jar:1.2-beta",
  :connector        =>"org.apache.geronimo.modules:geronimo-connector:jar:1.2-beta"
)
HIBERNATE           = [ "org.hibernate:hibernate:jar:3.1.2", "asm:asm:jar:1.5.3",
                        "antlr:antlr:jar:2.7.6rc1", "cglib:cglib:jar:2.1_3", "ehcache:ehcache:jar:1.1" ]
HOWL_LOGGER         = "howl:howl-logger:jar:0.1.11"
HSQLDB              = "hsqldb:hsqldb:jar:1.8.0.7"
JAVAX               = OpenStruct.new(
  :activation       =>"javax.activation:activation:jar:1.1",
  #:activation       =>"geronimo-spec:geronimo-spec-activation:jar:1.0.2-rc4",
  :connector        =>"org.apache.geronimo.specs:geronimo-j2ee-connector_1.5_spec:jar:1.0",
  :ejb              =>"org.apache.geronimo.specs:geronimo-ejb_2.1_spec:jar:1.1",
  :javamail         =>"geronimo-spec:geronimo-spec-javamail:jar:1.3.1-rc5",
  :jms              =>"geronimo-spec:geronimo-spec-jms:jar:1.1-rc4",
  :persistence      =>"javax.persistence:persistence-api:jar:1.0",
  :servlet          =>"org.apache.geronimo.specs:geronimo-servlet_2.4_spec:jar:1.0",
  :stream           =>"stax:stax-api:jar:1.0.1",
  :transaction      =>"org.apache.geronimo.specs:geronimo-jta_1.0.1B_spec:jar:1.0"
)
JAXEN               = "jaxen:jaxen:jar:1.1-beta-8"
JBI                 = "org.apache.servicemix:servicemix-jbi:jar:3.1-incubating"
JENCKS              = "org.jencks:jencks:jar:all:1.3"
JIBX                = "jibx:jibx-run:jar:1.1-beta3"
LOG4J               = "log4j:log4j:jar:1.2.13"
OPENJPA             = ["org.apache.openjpa:openjpa-all:jar:0.9.7-incubating-SNAPSHOT",
                       "net.sourceforge.serp:serp:jar:1.12.0"]
QUARTZ              = "quartz:quartz:jar:1.5.2"
SAXON               = group("saxon", "saxon-xpath", "saxon-dom", :under=>"net.sf.saxon", :version=>"8.7")
SERVICEMIX          = group("servicemix-core", "servicemix-shared", "servicemix-services", :under=>"org.apache.servicemix", :version=>"3.1-incubating")
SPRING              = group("spring-beans", "spring-context", "spring-core", "spring-jmx", :under=>"org.springframework", :version=>"2.0.1")
TRANQL              = [ "tranql:tranql-connector:jar:1.1", "axion:axion:jar:1.0-M3-dev", COMMONS.primitives ]
"regexp:regexp:jar:1.3"
WOODSTOX            = "woodstox:wstx-asl:jar:3.0.1"
WSDL4J              = "wsdl4j:wsdl4j:jar:1.6.1"
XALAN               = "org.apache.ode:xalan:jar:2.7.0"
XERCES              = "xerces:xercesImpl:jar:2.8.0"
XSTREAM             = "xstream:xstream:jar:1.2"
WS_COMMONS          = OpenStruct.new(
  :axiom            =>AXIOM,
  :neethi           =>"org.apache.ws.commons.neethi:neethi:jar:2.0",
  :xml_schema       =>"org.apache.ws.commons.schema:XmlSchema:jar:1.2"
)
XBEAN               = group("xbean-classloader", "xbean-kernel", "xbean-server", "xbean-spring", :under=>"org.apache.xbean", :version=>"2.8")
XMLBEANS            = "xmlbeans:xbean:jar:2.2.0"


repositories.remote << "http://pxe.intalio.org/public/maven2"
repositories.remote << "http://people.apache.org/repo/m2-incubating-repository"
repositories.remote << "http://repo1.maven.org/maven2"
repositories.remote << "http://people.apache.org/repo/m2-snapshot-repository"
repositories.deploy_to[:url] ||= "sftp://ode.intalio.org/var/www/public/maven2"


define "ode", :group=>"org.apache.ode", :version=>VERSION_NUMBER do

  compile.options.source = "1.5"
  compile.options.target = "1.5"
  manifest["Implementation-Vendor"] = "Apache Software Foundation"
  meta_inf << file("DISCLAIMER") << file("NOTICE")

  desc "ODE Axis Integration Layer"
  define "axis2" do
    compile.with projects("ode:bpel-api", "ode:bpel-connector", "ode:bpel-dao", "ode:bpel-epr", "ode:bpel-runtime",
      "ode:bpel-scheduler-quartz", "ode:bpel-schemas", "ode:bpel-store", "ode:utils"),
      AXIOM, AXIS2, COMMONS.logging, COMMONS.collections, DERBY, GERONIMO.kernel, GERONIMO.transaction,
      JAVAX.activation, JAVAX.servlet, JAVAX.stream, JAVAX.transaction, JENCKS, WSDL4J, XMLBEANS

    test.with project("ode:tools"), XERCES, WOODSTOX, AXIOM, WS_COMMONS.xml_schema, JAVAX.javamail
    test.exclude '*'

    package :jar
  end

  desc "ODE Axis2 Based Web Application"
  define "axis2-war" do
    libs = projects("ode:axis2", "ode:bpel-api", "ode:bpel-compiler", "ode:bpel-connector", "ode:bpel-dao",
      "ode:bpel-epr", "ode:bpel-obj", "ode:bpel-ql", "ode:bpel-runtime", "ode:bpel-scheduler-quartz",
      "ode:bpel-schemas", "ode:bpel-store", "ode:dao-hibernate", "ode:jacob", "ode:jca-ra", "ode:jca-server",
      "ode:utils", "ode:dao-jpa"),
      AXIS2_ALL, ANNONGEN, BACKPORT, COMMONS.codec, COMMONS.collections, COMMONS.fileupload, COMMONS.httpclient,
      COMMONS.lang, COMMONS.pool, DERBY, DERBY_TOOLS, JAXEN, JAVAX.activation, JAVAX.ejb, JAVAX.javamail,
      JAVAX.connector, JAVAX.jms, JAVAX.persistence, JAVAX.transaction, JAVAX.stream,  JIBX,
      GERONIMO.connector, GERONIMO.kernel, GERONIMO.transaction, LOG4J, OPENJPA, QUARTZ, SAXON, TRANQL,
      WOODSTOX, WSDL4J, WS_COMMONS.axiom, WS_COMMONS.neethi, WS_COMMONS.xml_schema, XALAN, XERCES, XMLBEANS

    package(:war).with(:libs=>libs).path("WEB-INF").tap do |web_inf|
      web_inf.merge project("ode:dao-jpa-ojpa-derby").package(:zip)
      web_inf.merge project("ode:dao-hibernate-db").package(:zip)
      web_inf.include project("ode:axis2").path_to("src/main/wsdl/*")
      web_inf.include project("ode:bpel-schemas").path_to("src/main/xsd/pmapi.xsd")
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
  end

  desc "ODE APIs"
  define "bpel-api" do
    compile.with projects("ode:utils", "ode:bpel-obj", "ode:bpel-schemas"), WSDL4J, COMMONS.logging
    package :jar
  end

  desc "ODE JCA connector"
  define "bpel-api-jca" do
    compile.with project("ode:bpel-api"), JAVAX.connector
    package :jar
  end

  desc "ODE BPEL Compiler"
  define "bpel-compiler" do
    compile.with projects("ode:bpel-api", "ode:bpel-obj", "ode:bpel-schemas", "ode:bpel-scripts", "ode:utils"),
      COMMONS.logging, JAVAX.stream, JAXEN, SAXON, WSDL4J, XALAN, XERCES
    package :jar
  end

  desc "ODE JCA Connector Implementation"
  define "bpel-connector" do
    compile.with projects("ode:bpel-api", "ode:bpel-api-jca", "ode:bpel-runtime", "ode:jca-ra", "ode:jca-server")
    package :jar
  end

  desc "ODE DAO Interfaces"
  define "bpel-dao" do
    compile.with project("ode:bpel-api")
    package :jar
  end

  desc "ODE Interface Layers Common"
  define "bpel-epr" do
    compile.with projects("ode:utils", "ode:bpel-dao", "ode:bpel-api"),
      COMMONS.logging, DERBY, JAVAX.transaction, GERONIMO.transaction, GERONIMO.connector, TRANQL, JAVAX.connector
    package :jar
  end

  desc "ODE BPEL Object Model"
  define "bpel-obj" do
    compile.with project("ode:utils"), SAXON, WSDL4J
    package :jar
  end

  desc "ODE BPEL Query Language"
  define "bpel-ql" do
    pkg_name = "org.apache.ode.ql.jcc"
    jjtree = jjtree("src/main/jjtree", :in_package=>pkg_name)
    compile.from javacc(jjtree, :in_package=>pkg_name), jjtree
    compile.with projects("ode:bpel-api", "ode:bpel-compiler", "ode:bpel-obj", "ode:jacob", "ode:utils")

    package :jar
  end

  desc "ODE Runtime Engine"
  define "bpel-runtime" do
    compile.from apt
    compile.with projects("ode:bpel-api", "ode:bpel-compiler", "ode:bpel-dao", "ode:bpel-obj", "ode:bpel-schemas",
      "ode:bpel-store", "ode:jacob", "ode:jacob-ap", "ode:utils"),
      COMMONS.logging, COMMONS.collections, JAXEN, JAVAX.persistence, JAVAX.stream, SAXON, WSDL4J, XMLBEANS

    test.compile.with projects("ode:bpel-scheduler-quartz", "ode:dao-jpa", "ode:dao-hibernate", "ode:bpel-epr"),
        BACKPORT, COMMONS.pool, COMMONS.lang, DERBY, JAVAX.connector, JAVAX.transaction, 
        GERONIMO.transaction, GERONIMO.kernel, GERONIMO.connector, TRANQL, HSQLDB, JAVAX.ejb,
        LOG4J, XERCES, Java::OpenJPA::REQUIRES, QUARTZ, XALAN
    test.junit.with HIBERNATE, DOM4J
    test.resources unzip(project("ode:dao-jpa-ojpa-derby").package(:zip)).into(path_to(compile.target, "derby-db"))

    package :jar
  end

  desc "ODE Quartz Interface"
  define "bpel-scheduler-quartz" do
    compile.with projects("ode:bpel-api", "ode:utils"), COMMONS.collections, COMMONS.logging, JAVAX.transaction, QUARTZ
    package :jar
  end

  desc "ODE Schemas"
  define "bpel-schemas" do
    compile_xml_beans "src/main/xsd/*.xsdconfig", "src/main/xsd"
    package :jar
  end

  desc "ODE BPEL Test Script Files"
  define "bpel-scripts" do
    package :jar
  end

  desc "ODE Process Store"
  define "bpel-store" do
    compile.with projects("ode:bpel-api", "ode:bpel-compiler", "ode:bpel-dao", "ode:bpel-obj", "ode:bpel-schemas",
      "ode:dao-hibernate", "ode:utils"),
      COMMONS.logging, JAVAX.persistence, JAVAX.stream, HIBERNATE, HSQLDB, XMLBEANS, XERCES, WSDL4J

    compile do 
      Java::Hibernate.xdoclet :sources=>compile.sources, :target=>compile.target, :excludedtags=>"@version,@author,@todo"
      open_jpa_enhance
    end
    test.with COMMONS.collections, COMMONS.lang, JAVAX.connector, JAVAX.transaction, DOM4J, LOG4J, 
      XERCES, XALAN, JAXEN, SAXON, OPENJPA
    package :jar
  end

  desc "ODE BPEL Tests"
  define "bpel-test" do
    compile.with projects("ode:bpel-api", "ode:bpel-compiler", "ode:bpel-dao", "ode:bpel-runtime", "ode:bpel-store", "ode:utils"),
      DERBY, WSDL4J

    test.with projects("ode:bpel-obj", "ode:dao-jpa", "ode:jacob", "ode:bpel-schemas",
      "ode:bpel-scripts", "ode:bpel-scheduler-quartz"),
      COMMONS.collections, COMMONS.lang, COMMONS.logging, DERBY, JAVAX.connector, JAVAX.persistence,
      JAVAX.stream, JAVAX.transaction, JAXEN, HSQLDB, LOG4J, OPENJPA, SAXON, XERCES, XMLBEANS, XALAN

    package :jar
  end

  desc "ODE Hibernate DAO Implementation"
  define "dao-hibernate" do
    compile.with projects("ode:bpel-api", "ode:bpel-dao", "ode:bpel-ql", "ode:utils"),
      COMMONS.lang, COMMONS.logging, JAVAX.transaction, HIBERNATE, DOM4J
    compile do
      Java::Hibernate.xdoclet :sources=>compile.sources, :target=>compile.target, :excludedtags=>"@version,@author,@todo"
    end

    test.with project("ode:bpel-epr"), BACKPORT, COMMONS.collections, COMMONS.lang, HSQLDB,
      GERONIMO.transaction, GERONIMO.kernel, GERONIMO.connector, JAVAX.connector, JAVAX.ejb, SPRING

    package :jar
  end

  desc "ODE Hibernate Compatible Databases"
  define "dao-hibernate-db" do

    predefined_for = lambda { |name| file("src/main/sql/tables_#{name}.sql") }
    properties_for = lambda { |name| file("src/main/sql/ode.#{name}.properties") }

    dao_hibernate = project("ode:dao-hibernate").compile.target
    bpel_store = project("ode:bpel-store").compile.target

    export_task = Java::Hibernate.schemaexport_task
    export = lambda do |properties, source, target|
      export_task.enhance([properties, source]) do |task| 
        task.ant.schemaexport(:properties=>properties.to_s, :quiet=>"yes", :text=>"yes", :delimiter=>";",
          :drop=>"no", :create=>"yes", :output=>path_to(target)) { fileset(:dir=>source.to_s) { include :name=>"**/*.hbm.xml" } }
      end
      file(target.to_s=>[properties, source]) { export_task.invoke }
    end

    build file_create("target") { |task| mkpath task.name }
    runtime_sql = export.call(properties_for[:derby], dao_hibernate, "target/runtime.sql") 
    store_sql = export.call(properties_for[:derby], bpel_store, "target/store.sql") 
    derby_sql = concat("target/derby.sql"=>[ predefined_for[:derby], runtime_sql, store_sql ])
    %w{ firebird hsql postgres sqlserver }.each do |db|
      partial = export.call(properties_for[db], dao_hibernate, "target/partial.#{db}.sql")
      build concat(path_to("target/#{db}.sql")=>[ predefined_for[db], partial ])
    end
    derby_db = Derby.create("target/derby/hibdb"=>derby_sql)

    build derby_db
    package :zip, :include=>derby_db
  end

  desc "ODE OpenJPA DAO Implementation"
  define "dao-jpa" do
    compile.with projects("ode:bpel-api", "ode:bpel-dao", "ode:utils"),
      COMMONS.collections, COMMONS.logging, JAVAX.connector, JAVAX.persistence, JAVAX.transaction,
      OPENJPA, XERCES
    compile { open_jpa_enhance }
    package :jar
  end

  desc "ODE OpenJPA Derby Database"
  define "dao-jpa-ojpa-derby" do
    # TODO: find if there's any way to simplify all of this.
    # Create the Derby SQL file using the OpenJPA mapping tool, and
    # append the Quartz DDL at the end.
    derby_xml = "src/main/descriptors/persistence.derby.xml"
    quartz_sql = "src/main/scripts/quartz-derby.sql"
    partial_sql = file("target/partial.sql"=>derby_xml) do |task|
      mkpath "target", :verbose=>false
      Java::OpenJPA.mapping_tool :properties=>derby_xml, :action=>"build", :sql=>task.name,
        :classpath=>projects("ode:bpel-store", "ode:dao-jpa", "ode:bpel-api", "ode:bpel-dao", "ode:utils" )
    end
    derby_sql = concat("target/derby.sql"=>[partial_sql, quartz_sql])
    derby_db = Derby.create("target/derby/jpadb"=>derby_sql)

    test.with projects("ode:bpel-api", "ode:bpel-dao", "ode:bpel-obj", "ode:bpel-epr", "ode:dao-jpa", "ode:utils"),
      BACKPORT, COMMONS.collections, COMMONS.lang, COMMONS.logging, GERONIMO.transaction,
      GERONIMO.kernel, GERONIMO.connector, HSQLDB, JAVAX.connector, JAVAX.ejb, JAVAX.persistence,
      JAVAX.transaction, LOG4J, OPENJPA, XERCES, WSDL4J

    build derby_db
    package :zip, :include=>derby_db
  end

  distro_common = lambda do |project, zip|
    zip.include meta_inf + ["RELEASE_NOTES", "README"].map { |f| project.parent.path_to(f) }
    zip.path("examples").include FileList["src/examples/**"]
    zip.merge project("ode:tools-bin").package(:zip)
    zip.path("lib").include artifacts(COMMONS.logging, COMMONS.codec, COMMONS.httpclient,
      COMMONS.pool, COMMONS.collections, JAXEN,
      SAXON, LOG4J, WSDL4J)
    projects("ode:utils", "ode:tools", "ode:bpel-compiler", "ode:bpel-api", "ode:bpel-obj", "ode:bpel-schemas").
      map(&:packages).flatten.each do |pkg|
      zip.include(pkg.to_s, :as=>"#{pkg.id}.#{pkg.type}", :path=>"lib")
    end
  end

  desc "ODE Axis2 Based Distribution"
  define "distro-axis2" do
    package(:zip).tap do |zip|
      distro_common.call(self, zip)
      zip.include project("ode:axis2-war").package(:war), :as=>"ode.war"
    end

    project("ode:axis2-war").task("start").enhance do |task|
      target = "#{task.path}/webapp/WEB-INF/processes"
      puts "Deploying processes to #{target}" if verbose
      verbose(false) do
        mkpath target
        cp_r FileList["src/examples/*"].to_a, target
        rm Dir.glob("#{target}/*.deployed")
      end
    end
  end

  desc "ODE JBI Based Distribution"
  define "distro-jbi" do
    package(:zip).tap do |zip|
      distro_common.call(self, zip)
      zip.path("jbi-component").include project("ode:jbi").package(:zip)
    end
  end

  desc "ODE JAva Concurrent OBjects"
  define "jacob" do
    compile.with projects("ode:utils", "ode:jacob-ap"), COMMONS.logging
    compile.from apt

    package :jar
  end

  desc "ODE Jacob APR Code Generation"
  define "jacob-ap" do
    compile.with Java.tools
    package :jar
  end

  desc "ODE JBI Integration Layer"
  define "jbi" do
    compile.with projects("ode:bpel-api", "ode:bpel-connector", "ode:bpel-dao", "ode:bpel-epr", "ode:bpel-obj",
      "ode:bpel-runtime", "ode:bpel-scheduler-quartz", "ode:bpel-schemas", "ode:bpel-store", "ode:utils"),
      COMMONS.logging, COMMONS.pool, JAVAX.transaction, JBI, LOG4J, WSDL4J, XERCES

    package(:jar)
    package(:jbi).tap do |jbi|
      libs = artifacts(package(:jar),
        projects("ode:bpel-api", "ode:bpel-api-jca", "ode:bpel-compiler", "ode:bpel-connector", "ode:bpel-dao",
        "ode:bpel-epr", "ode:jca-ra", "ode:jca-server", "ode:bpel-obj", "ode:bpel-ql", "ode:bpel-runtime",
        "ode:bpel-scheduler-quartz", "ode:bpel-schemas", "ode:bpel-store", "ode:dao-hibernate", "ode:dao-jpa",
        "ode:jacob", "ode:jacob-ap", "ode:utils"),
        ANT, BACKPORT, COMMONS.codec, COMMONS.collections, COMMONS.dbcp, COMMONS.lang, COMMONS.pool,
        COMMONS.primitives, DOM4J, HIBERNATE, HSQLDB, JAXEN, JAVAX.connector, JAVAX.ejb, JAVAX.jms,
        JAVAX.persistence, JAVAX.stream, JAVAX.transaction, LOG4J, OPENJPA, QUARTZ, SAXON, TRANQL,
        XALAN, XMLBEANS, XSTREAM, WSDL4J)

      jbi.component :type=>:service_engine, :name=>"OdeBpelEngine", :description=>self.comment
      jbi.component :class_name=>"org.apache.ode.jbi.OdeComponent", :delegation=>:self, :libs=>libs
      jbi.bootstrap :class_name=>"org.apache.ode.jbi.OdeBootstrap", :libs=>libs
      jbi.merge project("ode:dao-hibernate-db").package(:zip)
      jbi.merge project("ode:dao-jpa-ojpa-derby").package(:zip)
    end

    test.with projects("ode:dao-jpa", "ode:bpel-compiler", "ode:bpel-api-jca", "ode:jca-ra",
      "ode:jca-server", "ode:jacob"), 
      BACKPORT, COMMONS.lang, COMMONS.collections, DERBY, GERONIMO.connector, GERONIMO.kernel,
      GERONIMO.transaction, JAVAX.connector, JAVAX.ejb, JAVAX.persistence, JAVAX.stream,
      JAVAX.transaction, JAXEN, JBI, OPENJPA, QUARTZ, SAXON, SERVICEMIX, SPRING, TRANQL,
      XALAN, XBEAN, XMLBEANS, XSTREAM
    test.resources unzip(project("ode:dao-jpa-ojpa-derby").package(:zip)).into(path_to("target/smixInstallDir/install/ODE"))

  end

  desc "ODE JCA Resource Archive"
  define "jca-ra" do
    compile.with project("ode:utils"), JAVAX.connector
    package :jar
  end

  desc "ODE JCA Server"
  define "jca-server" do
    compile.with projects("ode:jca-ra", "ode:utils"), COMMONS.logging
    package :jar
  end

  desc "ODE Tools"
  define "tools" do
    compile.with projects("ode:bpel-compiler", "ode:utils"), ANT, COMMONS.httpclient, COMMONS.logging
    package :jar
  end

  desc "ODE Tools Binaries"
  define "tools-bin" do
    # Copy binary files over, set permissions on Linux files.
    bins = filter("src/main/dist/bin/*").into("target/bin").
      enhance { |task| chmod 0755, FileList[task.target.to_s + "/*.sh"], :verbose=>false }
    # Copy docs over.
    docs = filter("src/main/dist/doc/*").into("target/doc")

    build bins, docs
    package(:zip).include bins.target, docs.target
  end

  desc "ODE Utils"
  define "utils" do
    compile.with COMMONS.logging, COMMONS.pool, LOG4J, XERCES, JAVAX.stream
    package :jar
  end

end
