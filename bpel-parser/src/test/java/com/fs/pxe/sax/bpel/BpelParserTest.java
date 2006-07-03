/*
 * File:      $RCSfile$
 * Copyright: (C) 1999-2005 FiveSight Technologies Inc.
 *
 */
package com.fs.pxe.sax.bpel;

import com.fs.pxe.bom.api.EmptyActivity;
import com.fs.pxe.bom.api.Process;
import com.fs.pxe.bpel.parser.BpelParseException;
import com.fs.pxe.bpel.parser.BpelProcessBuilder;
import com.fs.pxe.bpel.parser.BpelProcessBuilderFactory;
import com.fs.pxe.sax.fsa.ParseError;
import com.fs.pxe.sax.fsa.ParseException;

import junit.framework.TestCase;

import org.xml.sax.InputSource;

public class BpelParserTest extends TestCase {
  BpelProcessBuilderFactory bpf;
  BpelProcessBuilder builder;

  InputSource ok_trivial;
  InputSource bad_trivial;

  public void setUp() throws Exception {
    bpf = BpelProcessBuilderFactory.newProcessBuilderFactory();
    builder = bpf.newBpelProcessBuilder();
    ok_trivial = new InputSource(getClass().getResource("ok_trivial.bpel").toExternalForm());
    bad_trivial = new InputSource(getClass().getResource("bad_trivial.bpel").toExternalForm());
  }

  public void testOkTrivial() throws Exception {
    Process proc = builder.parse(ok_trivial, "<<unknown>>");
    ParseError[] err = builder.getParseErrors();
    assertNotNull(proc);
    assertEquals(0,err.length);
    assertEquals(Process.BPEL_V110, proc.getBpelVersion());
    // These are the schema defaults.
    assertEquals(proc.getQueryLanguage(),"http://www.w3.org/TR/1999/REC-xpath-19991116");
    assertEquals(proc.getExpressionLanguage(),"http://www.w3.org/TR/1999/REC-xpath-19991116");
    assertEquals("empty", proc.getRootActivity().getType());
    assertTrue(EmptyActivity.class.isAssignableFrom(proc.getRootActivity().getClass()));
  }

  public void testBadTrivial() throws Exception {
    try {
      builder.parse(bad_trivial, "<<unknown>>");
      fail("expected exception due to schema violation.");
    } catch (BpelParseException bpe) {
      // validation will fail, and that should get rolled into the ParseContext
      assertTrue(bpe.getCause() instanceof ParseException);
    }
    ParseError[] err = builder.getParseErrors();
    assertTrue(err.length > 0);
  }

}
