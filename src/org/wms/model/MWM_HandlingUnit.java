/**
* Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.
* If you shall try to cheat and find a loophole in this license, then KARMA will exact your share,
* and your worldly gain shall come to naught and those who share shall gain eventually above you.
* In compliance with previous GPLv2.0 works of Jorg Janke, Low Heng Sin, Carlos Ruiz and contributors.
* This Module Creator is an idea put together and coded by Redhuan D. Oon (red1@red1.org)
*/
package org.wms.model;

import java.io.File;

import java.math.BigDecimal;

import java.sql.ResultSet;

import java.util.Properties;

import java.util.logging.Level; 

import org.compiere.print.ReportEngine;
import org.compiere.util.Msg;
import org.kanbanboard.model.MKanbanCard;
import org.ninja.component.DocAction;
import org.ninja.component.DocumentEngine;
import org.wms.model.X_WM_HandlingUnit;

public class MWM_HandlingUnit extends X_WM_HandlingUnit implements DocAction {
	public MWM_HandlingUnit(Properties ctx, int id, String trxName) {
		super(ctx, id, trxName);

		if (id==0){
			setDocStatus(DOCSTATUS_Drafted);
			setDocAction (DOCACTION_Prepare);
			setProcessed(false);
		}

	}

	public MWM_HandlingUnit(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName); 
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
 		DocumentEngine engine = new DocumentEngine (this, getDocStatus());
		return engine.processIt (processAction, getDocAction());
	}
 
	public boolean waitingConfirmation() {
		if (log.isLoggable(Level.INFO)) 
			log.info("waitingConfirmation - " + toString());
		setDocStatus(DOCSTATUS_WaitingConfirmation);
		return true; 
	}
 
	public boolean waitingPayment() {
		if (log.isLoggable(Level.INFO)) 
			log.info("waitingPayment - " + toString());
		setDocStatus(DOCSTATUS_WaitingPayment);
		return true; 
	}
 
	public boolean unlockIt() {
		if (log.isLoggable(Level.INFO)) 
			log.info("unlockIt - " + toString());
		setDocStatus(DOCSTATUS_Unknown);
		return true; 
	}
 
	public boolean invalidateIt() {
		if (log.isLoggable(Level.INFO)) log.info("invalidateIt - " + toString());
		setDocStatus(DOCSTATUS_Invalid);
		//red1 return false if condition disallow move - put in Error Message 
		//MKanbanCard.KDB_ErrorMessage = "";
		return true;
	}
 
	public String prepareIt() {
		if (log.isLoggable(Level.INFO)) log.info(toString());

		return DocAction.STATUS_InProgress; 
	}

 	public boolean approveIt() {
		if (log.isLoggable(Level.INFO)) log.info("approveIt - " + toString());
		setDocStatus(DOCSTATUS_Approved);
		return true; 
	}

 	public boolean rejectIt() { 
		setDocStatus(DOCSTATUS_NotApproved);
		return true;
	}

 	public String completeIt() {
  		return DocAction.STATUS_Completed;
	}
	
	public boolean voidIt() {
		setDocStatus(DOCSTATUS_Voided);
		return true;
	}

 	public boolean closeIt() {
 		setDocStatus(DOCSTATUS_Closed);
		return true;
	}

  	public boolean reverseCorrectIt() {
  		setDocStatus(DOCSTATUS_Reversed);
 		return true;
	}

 	public boolean reverseAccrualIt() {
		if (log.isLoggable(Level.INFO)) log.info(toString());
		setDocStatus(DOCSTATUS_Reversed);
		return true;
	}

 	public boolean reActivateIt() {
		if (log.isLoggable(Level.INFO)) log.info(toString());
		return true;
	}

 	public String getSummary() {
		// TODO Auto-generated method stub
		return null;
	}

 	public String getDocumentNo() {
		return Msg.getElement(getCtx(), X_WM_HandlingUnit.COLUMNNAME_WM_HandlingUnit_ID) + " " + getDocumentNo();
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
		ReportEngine re = ReportEngine.get (getCtx(), ReportEngine.ORDER, getWM_HandlingUnit_ID());
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

	@Override
	public boolean waitComplete() {
		setDocStatus(DOCSTATUS_WaitingConfirmation);
		return true;
	}
	@Override
	public boolean waitPayment() {
		setDocStatus(DOCSTATUS_WaitingPayment);
		return true;
	}
	@Override
	public boolean waitConfirmation() {
		setDocStatus(DOCSTATUS_WaitingConfirmation);
		return true;
	}
 
}

