package org.apache.ode.axis2.correlation;

import org.apache.ode.axis2.Axis2TestBase;
import org.apache.ode.axis2.DummyService;
import org.apache.ode.axis2.ODEConfigDirAware;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class CorrelationMultiTest extends Axis2TestBase implements ODEConfigDirAware {
    @Test(dataProvider="configs")
	public void testCorrelationMulti() throws Exception {
        final String bundleName = "TestCorrelationMulti";
        
        // deploy the required service
        server.deployService(DummyService.class.getCanonicalName());
        if (server.isDeployed(bundleName)) server.undeployProcess(bundleName);
        server.deployProcess(bundleName);

        new Thread() {
        	public void run() {
        		try {
        			Thread.sleep(3000);
        			server.sendRequestFile("http://localhost:8888/processes/correlationMultiTest",
                            bundleName, "testRequest2.soap");
        		} catch( Exception e ) {
        			fail(e.getMessage());
        		}
        	}
        }.start();

        try {
            String response = server.sendRequestFile("http://localhost:8888/processes/correlationMultiTest",
                    bundleName, "testRequest.soap");
            assertTrue(response.contains(">1;2;<"));
        } catch (Exception e) {
        	fail(e.getMessage());
        } finally {
            server.undeployProcess(bundleName);
        }
    }

	public String getODEConfigDir() {
		return getClass().getClassLoader().getResource("webapp").getFile() + "/WEB-INF/conf.jpa-derby";	
    }
}