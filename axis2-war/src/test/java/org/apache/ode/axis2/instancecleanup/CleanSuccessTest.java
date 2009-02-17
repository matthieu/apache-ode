package org.apache.ode.axis2.instancecleanup;

import org.apache.ode.bpel.dao.ProcessDAO;
import org.apache.ode.bpel.dao.ProcessInstanceDAO;
import org.apache.ode.bpel.iapi.ContextException;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.fail;

public class CleanSuccessTest extends CleanTestBase {
    @Test(dataProvider="configs")
    public void testCleanNone() throws Exception {
        go("TestCleanSuccess_None", 1, 0, 0, 0, 3, 0, 6, 2, 3, 6, 59, 76);
    }

    @Test(dataProvider="configs")
    public void testCleanInstance() throws Exception {
        try {
            go("TestCleanSuccess_Instance", 0, 0, 0, 0, 3, 0, 6, 2, 3, 6, 59, 70);
            fail("Shoud throw a runtime exception: you cannot use the instance category without the variables and correlations categories.");
        } catch(ContextException re) {}
    }

    @Test(dataProvider="configs")
    public void testCleanVariables() throws Exception {
        go("TestCleanSuccess_Variables", 1, 0, 0, 0, 3, 0, 6, 0, 0, 0, 59, 70);
    }

    @Test(dataProvider="configs")
    public void testCleanMessages() throws Exception {
        go("TestCleanSuccess_Messages", 1, 0, 0, 0, 0, 0, 0, 2, 3, 6, 59, 65);
    }

    @Test(dataProvider="configs")
    public void testCleanCorrelations() throws Exception {
        go("TestCleanSuccess_Correlations", 1, 0, 0, 0, 3, 0, 6, 2, 3, 6, 59, 76);
    }

    @Test(dataProvider="configs")
    public void testCleanEvents() throws Exception {
        go("TestCleanSuccess_Events", 1, 0, 0, 0, 3, 0, 6, 2, 3, 6, 0, 17);
    }

    @Test(dataProvider="configs")
    public void testCleanMessageCorrEvents() throws Exception {
        go("TestCleanSuccess_MessageCorrEvents", 1, 0, 0, 0, 0, 0, 0, 2, 3, 6, 0, 6);
    }

    @Test(dataProvider="configs")
    public void testCleanAll() throws Exception {
        go("TestCleanSuccess_All", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    protected void go(String bundleName, int instances, int activityRecoveries, int correlationSets, int faults, int exchanges, int routes, int messsages, int partnerLinks, int scopes, int variables, int events, int largeData) throws Exception {
        if (server.isDeployed(bundleName)) server.undeployProcess(bundleName);
        server.deployProcess(bundleName);
        ProcessDAO process = null;
        try {
            initialLargeDataCount = getLargeDataCount(0);
            
            server.sendRequestFile("http://localhost:8888/processes/FirstProcess/FirstProcess/FirstProcess/Client", bundleName, "testRequest.soap");
            process = assertInstanceCleanup(instances, activityRecoveries, correlationSets, faults, exchanges, routes, messsages, partnerLinks, scopes, variables, events, largeData);
        } finally {
            server.undeployProcess(bundleName);
            assertProcessCleanup(process);
        }
    }

    public String getODEConfigDir() {
        return getClass().getClassLoader().getResource("webapp").getFile() + "/WEB-INF/conf.jpa-derby"; 
    }
    
    protected ProcessInstanceDAO getInstance() {
        return JpaDaoConnectionFactoryImpl.getInstance();
    }
    
    @Override
    protected int getLargeDataCount(int echoCount) throws Exception {
        return echoCount;
    }
}