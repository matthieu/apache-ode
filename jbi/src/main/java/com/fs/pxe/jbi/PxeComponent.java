/*
 * File:      $RCSfile$
 * Copyright: (C) 1999-2005 FiveSight Technologies Inc.
 *
 */
package com.fs.pxe.jbi;

import javax.jbi.component.Component;
import javax.jbi.component.ComponentLifeCycle;
import javax.jbi.component.ServiceUnitManager;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.servicedesc.ServiceEndpoint;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

/**
 * This class implements javax.jbi.component.Component interface.
 */
public class PxeComponent implements Component {
	private PxeLifeCycle _lifeCycle;

	public PxeComponent() {
		_lifeCycle = new PxeLifeCycle();
	}

	public ComponentLifeCycle getLifeCycle() {
		return _lifeCycle;
	}

	public ServiceUnitManager getServiceUnitManager() {
		return _lifeCycle.getSUManager();
	}

	/**
	 * 
	 * @param ref
	 *          ServiceEndpoint object
	 * 
	 * @return Descriptor Object implementing javax.jbi.servicedesc.Descriptor
	 *         interface.
	 */
	public Document getServiceDescription(ServiceEndpoint ref) {
		return  _lifeCycle.getPxeContext().getServiceDescription(ref.getServiceName());
	}

	/**
	 * This method is called by JBI to check if this component, in the role of
	 * provider of the service indicated by the given exchange, can actually
	 * perform the operation desired. The consumer is described by the given
	 * capabilities, and JBI has already ensured that a fit exists between the set
	 * of required capabilities of the provider and the available capabilities of
	 * the consumer, and vice versa. This matching consists of simple set matching
	 * based on capability names only. <br>
	 * <br>
	 * Note that JBI assures matches on capability names only; it is the
	 * responsibility of this method to examine capability values to ensure a
	 * match with the consumer.
	 * 
	 * @param endpoint
	 *          the endpoint to be used by the consumer
	 * @param exchange
	 *          the proposed message exchange to be performed
	 * @return true if this provider component can perform the the given exchange
	 *         with the described consumer
	 */
	public boolean isExchangeWithConsumerOkay(ServiceEndpoint endpoint, MessageExchange exchange) {
		return true;
	}

	/**
	 * This method is called by JBI to check if this component, in the role of
	 * consumer of the service indicated by the given exchange, can actually
	 * interact with the the provider completely. Ths provider is described by the
	 * given capabilities, and JBI has already ensure that a fit exists between
	 * the set of required capabilities of the consumer and the available
	 * capabilities of the provider, and vice versa. This matching consists of
	 * simple set matching based on capability names only. <br>
	 * <br>
	 * Note that JBI assures matches on capability names only; it is the
	 * responsibility of this method to examine capability values to ensure a
	 * match with the provider.
	 * 
	 * @param exchange
	 *          the proposed message exchange to be performed
	 * @return true if this consurer component can interact with the described
	 *         provider to perform the given exchange
	 */
	public boolean isExchangeWithProviderOkay(ServiceEndpoint endpoint, MessageExchange exchange) {
		return true;
	}

	/**
	 * Resolve the given endpoint reference, given the capabilities of the given
	 * consumer. This is called by JBI when it is attempting to resolve the given
	 * endpoint reference on behalf of a component.
	 * 
	 * @param epr
	 *          the endpoint reference, in some XML dialect understood by the
	 *          appropriate component (usually a Binding Component).
	 * @return the service endpoint for the endpoint reference; <code>null</code>
	 *         if the endpoint reference cannot be resolved.
	 */
	public ServiceEndpoint resolveEndpointReference(DocumentFragment epr) {
    // We are an engine, so we don't have to worry about this.
		return null;
	}
}
