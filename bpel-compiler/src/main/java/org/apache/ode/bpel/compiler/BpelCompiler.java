package org.apache.ode.bpel.compiler;

import org.apache.ode.bpel.compiler.api.CompileListener;
import org.apache.ode.bpel.compiler.v2.ExtensionValidator;
import org.apache.ode.bpel.compiler.bom.Process;
import org.apache.ode.bpel.rapi.ProcessModel;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import java.util.Map;
import java.net.URI;

public interface BpelCompiler {

    public enum Version {
        BPEL11,
        BPEL20_DRAFT,
        BPEL20
    }

    void setResourceFinder(ResourceFinder rf);

    void setCompileListener(CompileListener cl);

    void setCustomProperties(Map<QName, Node> props);

    void setExtensionValidators(Map<QName, ExtensionValidator> validators);

    void addWsdlImport(URI from, URI wsdlImport, SourceLocation sloc);

    ProcessModel compile(Process p, ResourceFinder rf);

}
