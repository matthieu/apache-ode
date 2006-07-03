/*
 * File:      $RCSfile$
 * Copyright: (C) 1999-2005 FiveSight Technologies Inc.
 *
 */
package com.fs.sax.evt.attspec;

import com.fs.sax.evt.XmlAttributes;

public class OrSpec implements XmlAttributeSpec {
  
  XmlAttributeSpec _lhs;
  XmlAttributeSpec _rhs;
  
  public OrSpec(XmlAttributeSpec lhs, XmlAttributeSpec rhs) {
    _lhs = lhs;
    _rhs = rhs;
  }
  
  public boolean matches(XmlAttributes xatts) {
    return _lhs.matches(xatts) || _rhs.matches(xatts);
  }
}
