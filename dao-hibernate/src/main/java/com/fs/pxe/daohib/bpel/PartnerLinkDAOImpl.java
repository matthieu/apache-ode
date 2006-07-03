package com.fs.pxe.daohib.bpel;

import javax.xml.namespace.QName;

import com.fs.pxe.bpel.dao.PartnerLinkDAO;
import com.fs.pxe.daohib.SessionManager;
import com.fs.pxe.daohib.bpel.hobj.HPartnerLink;
import com.fs.pxe.daohib.hobj.HLargeData;
import com.fs.utils.DOMUtils;
import org.w3c.dom.Element;

/**
 * Hibernate based {EndpointReferenceDAO} implementation. can either be related
 * to a scope (when it's specific to a scope instance, for example because it
 * has been assigned during the instance execution) or to a process definition
 * (general endpoint configuration).
 */
public class PartnerLinkDAOImpl extends HibernateDao implements PartnerLinkDAO {

  /** Cached copy of my epr */
  private Element _myEPR;
  /** Cached copy of partner epr.*/
  private Element _partnerEPR;

  private HPartnerLink _self;

  public PartnerLinkDAOImpl(SessionManager sessionManager, HPartnerLink hobj) {
    super(sessionManager, hobj);
    _self = hobj;
  }

  public String getPartnerLinkName() {
    return _self.getLinkName();
  }

  public String getPartnerRoleName() {
    return _self.getPartnerRole();
  }

  public String getMyRoleName() {
    return _self.getMyRole();
  }

  public int getPartnerLinkModelId() {
    return _self.getModelId();
  }

  public QName getMyRoleServiceName() {
    return _self.getServiceName() == null ? null : QName.valueOf(_self
        .getServiceName());
  }

  public void setMyRoleServiceName(QName svcName) {
    _self.setServiceName(svcName == null ? null : svcName.toString());
    update();
  }

  public Element getMyEPR() {
    if (_myEPR != null)
      return _myEPR;
    if (_self.getMyEPR() == null)
      return null;
    try {
      return DOMUtils.stringToDOM(_self.getMyEPR().getText());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void setMyEPR(Element val) {
    _myEPR = val;
    if (_self.getMyEPR() != null)
      _sm.getSession().delete(_self.getMyEPR());
    if (val == null) {
      _self.setMyEPR(null);
    } else {
      HLargeData ld = new HLargeData(DOMUtils.domToString(val));
      getSession().save(ld);
      _self.setMyEPR(ld);
    }
    getSession().update(_self);
  }

  public Element getPartnerEPR() {
    if (_partnerEPR != null)
      return _partnerEPR;
    if (_self.getPartnerEPR() == null)
      return null;
    try {
      return _partnerEPR = DOMUtils.stringToDOM(_self.getPartnerEPR().getText());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void setPartnerEPR(Element val) {
    _partnerEPR = val;
    if (_self.getPartnerEPR() != null)
      _sm.getSession().delete(_self.getPartnerEPR());
    if (val == null) {
      _self.setPartnerEPR(null);
    } else {
      HLargeData ld = new HLargeData(DOMUtils.domToString(val));
      getSession().save(ld);
      _self.setPartnerEPR(ld);
    }
    getSession().update(_self);
  }

}
