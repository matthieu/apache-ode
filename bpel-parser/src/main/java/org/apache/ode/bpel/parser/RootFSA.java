/*
 * File:      $RCSfile$
 * Copyright: (C) 1999-2005 FiveSight Technologies Inc.
 *
 */
package org.apache.ode.bpel.parser;

import org.apache.ode.bom.api.Process;
import org.apache.ode.sax.fsa.*;
import org.apache.ode.sax.fsa.bpel_1_1.BpelGraph_1_1;
import org.apache.ode.sax.fsa.bpel_2_0.BpelGraph_2_0;
import org.apache.ode.sax.evt.Characters;
import org.apache.ode.sax.evt.SaxEvent;

class RootFSA extends FSA {
	
  private static final String START = "START_STATE";
  private static final String PROCESS_11 = "11PROCESS";
  private static final String PROCESS_20 = "20PROCESS";
  
  private BpelStartState _initialState;
  private GraphProvider _11Graph = new BpelGraph_1_1();
  private GraphProvider _20Graph = new BpelGraph_2_0();
  
	RootFSA() {
    super(new RootGraphProvider());
    reset();
	}

  void reset(){
    setGraphProvider(new RootGraphProvider());
    setStart(START,_initialState = new BpelStartState(getParseContext()));
  }
  
  Process getProcess(){
    return _initialState.getProcess();
  }
  
  protected void onStateChange(String fromState, String toState)
      throws ParseException {
    if (fromState.equals(START)) {
      if (toState.equals(PROCESS_11)) {
        setGraphProvider(_11Graph);
      } else if (toState.equals(PROCESS_20)) {
        setGraphProvider(_20Graph);
      } else {
        // TODO: Internationalize.
        getParseContext().parseError(ParseError.FATAL, "PARSER_FATAL",
            "This parser can only consume either 1.1 or 2.0 BPEL processes.");
      }
    }
  }
  
  private static final class RootGraphProvider extends AbstractGraphProvider {
    RootGraphProvider(){
      addStateFactory(PROCESS_11, BpelGraph_1_1.getRootStateFactory());
      addStateFactory(PROCESS_20, BpelGraph_2_0.getRootStateFactory());
      addQNameEdge(START, PROCESS_11, BpelGraph_1_1.get11QName("process"));
      addQNameEdge(START, PROCESS_20, BpelGraph_2_0.get20QName("process")); 
    }
  }
  
  private static final class BpelStartState extends AbstractState {

    private Process _process;
    
    public BpelStartState(ParseContext pc) {
      super(pc);
    }
    
    /**
     * @see org.apache.ode.sax.fsa.State#handleSaxEvent(org.apache.ode.sax.evt.SaxEvent)
     */
    public void handleSaxEvent(SaxEvent se) throws ParseException {
      if (se.getType() == SaxEvent.CHARACTERS
          &&
          ((Characters)se).getContent().trim().length() == 0)
      {
        return;
      }
      String msg = "Expected a process element in the namespace " + 
      BpelProcessBuilder.BPEL4WS_NS + " or " + BpelProcessBuilder.WSBPEL2_0_NS + " instead of ";
      if (se.getType() == SaxEvent.CHARACTERS) {
        msg += " non-whitespace characters.";
      } else {
        msg += se.toString();
      }
      getParseContext().parseError(ParseError.ERROR, se, "PARSER_ERROR", msg);
    }

    public Process getProcess() {
      return _process;
    }
    
    /**
     * @see org.apache.ode.sax.fsa.State#handleChildCompleted(org.apache.ode.sax.fsa.State)
     */
    public void handleChildCompleted(State pn) throws ParseException {
      if(pn instanceof BpelProcessState) {
        _process = ((BpelProcessState)pn).getProcess();
      } else {
        throw new IllegalStateException("The parser should not normally complete when processing a non-process.");
      }
    }

    public int getType() {
      return -1;
    }
    
    /**
     * @see org.apache.ode.sax.fsa.State#getFactory()
     */
    public StateFactory getFactory() {
      throw new UnsupportedOperationException("getFactory() should never be called on "
          + BpelStartState.class.getName());
    }
  }
}
