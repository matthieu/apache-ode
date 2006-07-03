/*
 * File:      $RCSfile$
 * Copyright: (C) 1999-2005 FiveSight Technologies Inc.
 *
 */
package com.fs.pxe.bpel.iapi;

import java.io.IOException;
import java.net.URI;

import javax.xml.namespace.QName;

import com.fs.pxe.bpel.dao.BpelDAOConnectionFactory;
import com.fs.pxe.bpel.pmapi.BpelManagementFacade;


/**
 * Interface implemented by the BPEL server. Provides methods for
 * life-cycle management.
 */
public interface BpelServer {

  /**
   * Configure the {@BpelEngine} with a message-exchange context. BPEL
   * engine uses this context to initiate communication with external
   * services.
   * @see MessageExchangeContext
   * @param mexContext {@link MessageExchangeContext} implementation
   */
  void setMessageExchangeContext(MessageExchangeContext mexContext) 
    throws BpelEngineException; 
  
  /**
   * Configure the {@BpelEngine} with a scheduler.  
   */
  void setScheduler(Scheduler scheduler)
    throws BpelEngineException; 
  

  /**
   * Configure the {@BpelEngine} with an endpoint-reference (EPR) context.
   * BPEL engine uses this context to EPRs.
   * @see EndpointReferenceContext
   * @param eprContext {@link EndpointReferenceContext} implementation
   */  
  void setEndpointReferenceContext(EndpointReferenceContext eprContext)
    throws BpelEngineException; 

  
  /**
   * Set the DAO connection factory. The DAO is used by the BPEL engine
   * to persist information about active processes.
   * @param daoCF {@link BpelDAOConnectionFactory} implementation.
   */
  void setDaoConnectionFactory(BpelDAOConnectionFactory daoCF)
    throws BpelEngineException; 

  
  /**
   * Initialize the BPEL engine. The various contexts needed by the
   * engine must be configured before this method is called. 
   */
  void init()
    throws BpelEngineException; 

  /**
   * Start the BPEL engine. The BPEL engine will not execute process
   * instances until it is started.
   */
  void start()
    throws BpelEngineException; 


  /**
   * Stop the BPEL engine: results in the cessation of process
   * execution.
   */
  void stop()
    throws BpelEngineException; 

            

  /**
   * Called to shutdown the BPEL egnine. 
   */
  void shutdown()
    throws BpelEngineException; 

  
  /**
   * Get the {@link BpelEngine} interface.
   * @return {@link BpelEngine} implementation
   */
  BpelEngine getEngine();
  
  /**
   * Deploy a process.
   * @param pid process identifier (unique)
   * @param deployURI location of deployment bundle
   * @throws IOException
   * @throws BpelEngineException
   */
  void deploy(QName pid, URI deployURI) throws IOException,
    BpelEngineException;
  
  /**
   * Undeploy a process.
   * @param pid
   * @return
   */
  boolean undeploy(QName pid);
  
  /**
   * Activate a process.
   * @param pid process to activate
   * @param sticky is this change sticky, i.e. will the process be activated
   *        on restart.
   * @throws BpelEngineException
   */
  void activate(QName pid, boolean sticky) throws BpelEngineException;
  
  /**
   * Deactivate a process.
   * @param pid process to deactivate
   * @param sticky whether the change is sticky, i.e. will the process be 
   *        deactivated on restart
   * @throws BpelEngineException
   */
  void deactivate(QName pid, boolean sticky) throws BpelEngineException;
  
  /**
   * Get the BPEL management interface.
   * @return
   */
  BpelManagementFacade getBpelManagementFacade();
  
}
