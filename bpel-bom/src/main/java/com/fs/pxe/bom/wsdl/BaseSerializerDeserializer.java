/*
 * File:      $RCSfile$
 * Copyright: (C) 1999-2005 FiveSight Technologies Inc.
 *
 */
package com.fs.pxe.bom.wsdl;

import com.fs.utils.DOMUtils;
import com.fs.utils.msg.MessageBundle;
import com.ibm.wsdl.util.xml.XPathUtils;

import javax.wsdl.WSDLException;
import javax.xml.namespace.QName;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Base class for WSDL4J serializers.
 */
class BaseSerializerDeserializer {
  protected static final Messages __msgs = MessageBundle.getMessages(Messages.class);

  /**
   * Dereference a qualified name given in the "ns:name" form using the namespace
   * context of a given element.
   *
   * @param prefixedQNameStr qualified name, represented as a prefixed string
   * @param context context element
   * @return a resolved {@link QName}
   * @throws javax.wsdl.WSDLException in case of resolution error (e.g. undefined prefix)
   */
  protected static QName derefQName(String prefixedQNameStr, Element context)
                          throws WSDLException {
    int idx = prefixedQNameStr.indexOf(":");
    String uri;

    if (idx == -1) {
      uri = DOMUtils.getNamespaceURIFromPrefix(context, null);
    } else {
      if (idx >= prefixedQNameStr.length() || idx == 0) {
        String msg = __msgs.msgMalformedQName(prefixedQNameStr);
        throw new WSDLException(WSDLException.INVALID_WSDL, msg);
      }

        // Look up the prefix from the namespaces defined *at the element*.
      String prefix = prefixedQNameStr.substring(0, idx);
      uri = DOMUtils.getMyNSContext(context).getNamespaceURI(prefix);

      if (uri == null) {
        String msg = __msgs.msgInvalidNamespacePrefix(prefix);
        throw new WSDLException(WSDLException.INVALID_WSDL, msg);
      }
    }

    return new QName(uri, prefixedQNameStr.substring(idx + 1, prefixedQNameStr.length()));
  }


  /**
   * Ensure that a top-level extensibility element does not occur before other WSDL
   * declarations (messages, port types, etc.).
   *
   * @param el the extensibility element
   * @throws javax.wsdl.WSDLException if the requirements of the WSDL schema are violated.
   */
  static void validateExtensibilityElementContext(Element el) throws WSDLException {
    Node n = el.getParentNode();
    if (n == null || n.getNodeType() != Node.ELEMENT_NODE) {
      WSDLException we = new WSDLException(WSDLException.OTHER_ERROR,
          __msgs.msgCannotBeDocumentRootElement(DOMUtils.getElementQName(el).toString()));
      we.setLocation(XPathUtils.getXPathExprFromNode(el));
      throw we;
    }
    Element def = (Element) n;
    if (def.getNamespaceURI() == null || !def.getNamespaceURI().equals(DOMUtils.WSDL_NS)
      || !def.getLocalName().equals(DOMUtils.WSDL_ROOT_ELEMENT))
    {
      WSDLException we =  new WSDLException(WSDLException.OTHER_ERROR,
          __msgs.msgMustBeChildOfDef(DOMUtils.getElementQName(el).toString()));
      we.setLocation(XPathUtils.getXPathExprFromNode(el));
      throw we;
    }
    /*
     * NOTE:
     * This check was originally put in to match the W3C version of the WSDL
     * schema, but the WS-I version is more permissive.  Leaving this check out
     * complies with the WS-I version, which is preferable.
     */
//    n = el.getNextSibling();
//    while (n != null) {
//      if (n.getNamespaceURI() != null && n.getNamespaceURI().equals(DOMUtils.WSDL_NS)) {
//        WSDLException we = new WSDLException(WSDLException.INVALID_WSDL,
//            MSGS.msgExtensibilityElementsMustBeLast(DOMUtils.getElementQName(el).toString()));
//        we.setLocation(XPathUtils.getXPathExprFromNode(el));
//        throw we;
//      }
//      n = n.getNextSibling();
//    }
  }

  protected String getAttribute(Element element, String attributeName) {
    Attr attribute = element.getAttributeNode(attributeName);
    if (attribute == null)
      return null;
    return attribute.getValue();

  }
}
