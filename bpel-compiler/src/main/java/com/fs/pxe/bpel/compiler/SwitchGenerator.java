/*
 * File:      $RCSfile$
 * Copyright: (C) 1999-2005 FiveSight Technologies Inc.
 *
 */



package com.fs.pxe.bpel.compiler;

import com.fs.pxe.bom.api.Activity;
import com.fs.pxe.bom.api.SwitchActivity;
import com.fs.pxe.bpel.o.OActivity;
import com.fs.pxe.bpel.o.OSwitch;

import java.util.Iterator;


/**
 * Generates code for the <code>&lt;switch&gt;</code> activities.
 */
class SwitchGenerator extends DefaultActivityGenerator {
  public OActivity newInstance(Activity src) {
    return new OSwitch(_context.getOProcess());
  }

  public void compile(OActivity output, Activity src) {
    OSwitch oswitch = (OSwitch) output;
    SwitchActivity switchDef = (SwitchActivity)src;

    for (Iterator<SwitchActivity.Case> i = switchDef.getCases().iterator(); i.hasNext();) {
      SwitchActivity.Case ccase =  i.next();
      OSwitch.OCase ocase = new OSwitch.OCase(_context.getOProcess());
      ocase.activity = _context.compile(ccase.getActivity());
      ocase.expression = ccase.getCondition() == null ? _context.constantExpr(true) : _context.compileExpr(ccase.getCondition());
      oswitch.addCase(ocase);
    }
  }
}
