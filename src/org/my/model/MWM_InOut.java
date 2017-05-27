/**
* Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.
* If you shall try to cheat and find a loophole in this license, then KARMA will exact your share,
* and your worldly gain shall come to naught and those who share shall gain eventually above you.
* In compliance with previous GPLv2.0 works of Jorg Janke, Low Heng Sin, Carlos Ruiz and contributors.
* This Module Creator is an idea put together and coded by Redhuan D. Oon (red1@red1.org)
*/
package org.my.model;

import java.io.File;

import java.math.BigDecimal;

import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;

import java.util.logging.Level;

import org.my.model.X_WM_InOut;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MOrder;
import org.compiere.model.ModelValidationEngine;

import org.compiere.model.ModelValidator;
import org.compiere.model.Query;
import org.compiere.print.ReportEngine;

import org.compiere.process.DocumentEngine;

import org.compiere.process.DocAction;
import org.compiere.util.Env;
import org.compiere.util.Msg;


public class MWM_InOut extends X_WM_InOut implements DocAction {
	public MWM_InOut(Properties ctx, int id, String trxName) {
		super(ctx, id, trxName);

		if (id==0){
			setDocStatus(DOCSTATUS_Drafted);

			setDocAction (DOCACTION_Prepare);

			setProcessed(false);

		}

	}

	public MWM_InOut(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);

		// TODO Auto-generated constructor stub
	}

	private static final long serialVersionUID = 1L;
	
	/**	Process Message 			*/
	private String			m_processMsg = null;

	private boolean			m_justPrepared = false;

	
	protected boolean beforeSave (boolean newRecord)
	{
		return super.beforeSave(newRecord);

	}

	protected boolean beforeDelete() {	 
		return super.beforeDelete();

	}

	protected boolean afterSave (boolean newRecord, boolean success)
	{
		return super.afterSave(newRecord, success);

	}
 
	protected boolean afterDelete(boolean success) {
		 
		return super.afterDelete(success);

	}
  
 
	public boolean processIt(String processAction) throws Exception {
		m_processMsg = null;

		DocumentEngine engine = new DocumentEngine (this, getDocStatus());

		return engine.processIt (processAction, getDocAction());

	}
 
	public boolean unlockIt() {
		if (log.isLoggable(Level.INFO)) 
			log.info("unlockIt - " + toString());


		return true;

	}
 
	public boolean invalidateIt() {
		if (log.isLoggable(Level.INFO)) log.info("invalidateIt - " + toString());

		setDocAction(DOCACTION_Prepare);

		return true;

	}
 
	public String prepareIt() {
		if (log.isLoggable(Level.INFO)) log.info(toString());

		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_PREPARE);

		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;
		
		//Create Material Receipt process    
		MInOut inout = null;
		
		List<MWM_InOutLine> lines = new Query(Env.getCtx(),MWM_InOutLine.Table_Name,MWM_InOutLine.COLUMNNAME_WM_InOut_ID+"=?",get_TrxName())
				.setParameters(this.get_ID()).list();
		
		//holder for separate M_InOut according to different C_Order
		int c_Order_Holder = 0;
		for (MWM_InOutLine line:lines){
			if (line.getM_InOutLine_ID()>0)
				throw new AdempiereException("Already has Shipment/Receipt record!");//already done before
				
			if (line.getWM_DeliveryScheduleLine().getC_OrderLine().getC_Order_ID()!=c_Order_Holder){
				if (inout!=null){
					saveM_InOut(inout,lines);
				}
				//create new MInOut  as C_Order_ID has changed
				inout = new MInOut(Env.getCtx(),0,get_TrxName());
				saveM_InOut(inout,lines);
				c_Order_Holder = line.getWM_DeliveryScheduleLine().getC_OrderLine().getC_Order_ID();
			}
			MInOutLine ioline = new MInOutLine(inout);
			ioline.setC_OrderLine_ID(line.getC_OrderLine_ID());
			ioline.setM_Product_ID(line.getM_Product_ID());
			ioline.setC_UOM_ID(line.getC_UOM_ID());
			ioline.setM_Locator_ID(line.getM_Locator_ID());
			ioline.setQtyEntered(line.getQtyPicked());
			ioline.setM_Warehouse_ID(line.getM_Locator().getM_Warehouse_ID());
			ioline.saveEx(get_TrxName());		
			//populate back WM_InOutLine with M_InOutLine_ID
			line.setM_InOutLine_ID(ioline.get_ID());
			line.saveEx(get_TrxName());
			//
		}
		if (inout!=null){
			saveM_InOut(inout,lines);
		}
	
 		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_PREPARE);

		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;

 		m_justPrepared = true;

		if (!DOCACTION_Complete.equals(getDocAction()))
			setDocAction(DOCACTION_Complete);

		return DocAction.STATUS_InProgress;

	}

	private void saveM_InOut(MInOut inout,List<MWM_InOutLine> lines) {
		if (inout.getC_Order_ID()>0)
			return;
		MOrder order = (MOrder) lines.get(0).getC_OrderLine().getC_Order();
		inout.setIsSOTrx(order.isSOTrx());
		inout.setC_Order_ID(order.getC_Order_ID());
		inout.setC_BPartner_ID(order.getC_BPartner_ID());
		inout.setC_BPartner_Location_ID(order.getC_BPartner_Location_ID());
		inout.setM_Warehouse_ID(order.getM_Warehouse_ID());
		inout.setMovementDate(lines.get(0).getUpdated());
		if (inout.isSOTrx())
			inout.setMovementType(MInOut.MOVEMENTTYPE_CustomerShipment);
		else
			inout.setMovementType(MInOut.MOVEMENTTYPE_VendorReceipts);
		inout.setAD_Org_ID(Env.getAD_Org_ID(Env.getCtx()));
		inout.setDocAction(DOCACTION_Prepare);
		if (inout.isSOTrx())
			inout.setC_DocType_ID(MDocType.DOCBASETYPE_MaterialDelivery);
		else
			inout.setC_DocType_ID(MDocType.DOCBASETYPE_MaterialReceipt);
		inout.setDateOrdered(order.getDateOrdered());
		inout.setDateReceived(lines.get(0).getWM_DeliveryScheduleLine().getWM_DeliverySchedule().getDateDelivered());
		inout.setPOReference(order.getPOReference());
		inout.saveEx(get_TrxName()); //save previous one before new one
	}

 	public boolean approveIt() {
		if (log.isLoggable(Level.INFO)) log.info("approveIt - " + toString());

		setIsApproved(true);

		return true;

	}

 	public boolean rejectIt() {
		if (log.isLoggable(Level.INFO)) log.info(toString());

		setIsApproved(false);

		return true;

	}

 	public String completeIt() {
		//	Just prepare
		if (!m_justPrepared)
		{
			String status = prepareIt();

			m_justPrepared = false;

			if (!DocAction.STATUS_InProgress.equals(status))
				return status;

		}

 		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_COMPLETE);

		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;

		
		//	Implicit Approval
		if (!isApproved())
			approveIt(); 
		
		if (log.isLoggable(Level.INFO)) log.info(toString());

		StringBuilder info = new StringBuilder();
		
		String valid = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_COMPLETE);

		if (valid != null)
		{
			if (info.length() > 0)
				info.append(" - ");

			info.append(valid);

			m_processMsg = info.toString();

			return DocAction.STATUS_Invalid;

		}


		setProcessed(true);
	
		m_processMsg = info.toString();

		//
		setDocAction(DOCACTION_Close);

		return DocAction.STATUS_Completed;

	}

	/**
	 * 	Document Status is Complete or Closed
	 *	@return true if CO, CL or RE
	 */
	public boolean isComplete()
	 {
	 	String ds = getDocStatus();

	 	return DOCSTATUS_Completed.equals(ds) 
	 		|| DOCSTATUS_Closed.equals(ds)
	 		|| DOCSTATUS_Reversed.equals(ds);

	 }
	//	isComplete
	
	public boolean voidIt() {
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_VOID);

		if (m_processMsg != null)
			return false;

 		setProcessed(true);

		setDocAction(DOCACTION_None);

		return true;

	}

 	public boolean closeIt() {
		if (log.isLoggable(Level.INFO)) log.info("closeIt - " + toString());

		// Before Close
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_CLOSE);

		if (m_processMsg != null)
			return false;

 		setProcessed(true);

		setDocAction(DOCACTION_None);

		// After Close
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_CLOSE);

		if (m_processMsg != null)
			return false;

		return true;

	}

  	public boolean reverseCorrectIt() {
		if (log.isLoggable(Level.INFO)) log.info(toString());

		// Before reverseCorrect
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REVERSECORRECT);

		if (m_processMsg != null)
			return false;

 		// After reverseCorrect
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REVERSECORRECT);

		if (m_processMsg != null)
			return false;

 		return voidIt();

	}

 	public boolean reverseAccrualIt() {
		if (log.isLoggable(Level.INFO)) log.info(toString());

		// Before reverseAccrual
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REVERSEACCRUAL);

		if (m_processMsg != null)
			return false;

 		// After reverseAccrual
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REVERSEACCRUAL);

		if (m_processMsg != null)
			return false;

 		setProcessed(true);

		setDocStatus(DOCSTATUS_Reversed);
		//	 may come from void
		setDocAction(DOCACTION_None);

		return true;

	}

 	public boolean reActivateIt() {
		if (log.isLoggable(Level.INFO)) log.info(toString());

		// Before reActivate
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REACTIVATE);

		if (m_processMsg != null)
			return false;

 		// After reActivate
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REACTIVATE);

		if (m_processMsg != null)
			return false;

 		setDocAction(DOCACTION_Complete);

		setProcessed(false);

		return true;

	}

 	public String getSummary() {
		// TODO Auto-generated method stub
		return null;

	}

 	public String getDocumentNo() {
		return Msg.getElement(getCtx(), X_WM_InOut.COLUMNNAME_WM_InOut_ID) + " " + getDocumentNo();

 	}

 	public String getDocumentInfo() {
		// TODO Auto-generated method stub
		return null;

	}

 	public File createPDF() {
		try
		{
			File temp = File.createTempFile(get_TableName()+get_ID()+"_", ".pdf");

			return createPDF (temp);

		}

		catch (Exception e)
		{
			log.severe("Could not create PDF - " + e.getMessage());

		}

		return null;

	}

 	/**
	 * 	Create PDF file
	 *	@param file output file
	 *	@return file if success
	 */
	public File createPDF (File file)
	{
		ReportEngine re = ReportEngine.get (getCtx(), ReportEngine.ORDER, getWM_InOut_ID());

		if (re == null)
			return null;

		return re.getPDF(file);

	}
	//	createPDF
 	public String getProcessMsg() {
		return m_processMsg;

	}

 	public int getDoc_User_ID() {
		// TODO Auto-generated method stub
		return 0;

	}

 	public int getC_Currency_ID() {
		// TODO Auto-generated method stub
		return 0;

	}

 	public BigDecimal getApprovalAmt() {
		// TODO Auto-generated method stub
		return null;

	}

 
}

