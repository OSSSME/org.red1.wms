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
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;

import java.util.logging.Level;

import org.wms.model.X_WM_InOut;
import org.wms.process.Utils;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MMovement;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MUOMConversion;
import org.compiere.model.ModelValidationEngine;

import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.print.ReportEngine;

import org.compiere.process.DocumentEngine;

import org.compiere.process.DocAction;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.TimeUtil;

/**
 * During Completion, DeliverySchedule must be Received to proceed. 
 * WM_EmptyStorage Vacant and PercentageAvailable are updated
 * WM_EmptyStorageLines are affected
 * M_InOut Shipment/Receipt OR M_Movement is created
 * @author red1
 *
 */
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
	}

	private static final long serialVersionUID = 1L;
	
	/**	Process Message 			*/
	private String			m_processMsg = null;

	private boolean			m_justPrepared = false;

	Utils util = new Utils(get_TrxName());

	private BigDecimal packFactor=Env.ONE;
	private BigDecimal boxConversion=Env.ONE;
	private BigDecimal currentUOM=Env.ONE;
	
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
	
 		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_PREPARE);

		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;

 		m_justPrepared = true;

		if (!DOCACTION_Complete.equals(getDocAction()))
			setDocAction(DOCACTION_Complete);

		return DocAction.STATUS_InProgress;

	}

	private MMovement createConsignmentMovement(MWM_InOut wio) {
		PO po = new Query(getCtx(),"WM_Consignment",MWM_InOut.COLUMNNAME_WM_DeliverySchedule_ID+"=?",get_TrxName())
				.setParameters(wio.get_ID())
				.first();
		if (po==null)
			throw new AdempiereException("No Consignment Found");
		MMovement move = new MMovement(getCtx(), 0, get_TrxName());
		move.setDescription(wio.getName()+" CONSIGNMENT");
		move.setC_BPartner_ID(wio.getC_BPartner_ID());
		move.setC_Project_ID(po.get_ValueAsInt(MMovement.COLUMNNAME_C_Project_ID));
		move.setC_Campaign_ID(po.get_ValueAsInt(MMovement.COLUMNNAME_C_Campaign_ID));
		move.saveEx(get_TrxName());
		//create Movement Lines
		List<MWM_InOutLine>wiolines = new Query(getCtx(),MWM_InOutLine.Table_Name,MWM_InOutLine.COLUMNNAME_WM_InOut_ID+"=?",get_TrxName())
				.setParameters(wio.get_ID())
				.list();
		for (MWM_InOutLine wioline:wiolines) {
			MWM_DeliveryScheduleLine del = (MWM_DeliveryScheduleLine) wioline.getWM_DeliveryScheduleLine();
			//get WMEmptyStorage from WIOLine.MLocator
			MWM_EmptyStorage empty = new Query(getCtx(),MWM_EmptyStorage.Table_Name,MWM_EmptyStorage.COLUMNNAME_M_Locator_ID+"=?",get_TrxName())
					.setParameters(wioline.getM_Locator_ID()).first();
			processWMSStorage(wioline,del,util);
		}
		return move;
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
		inout.setC_Project_ID(order.getC_Project_ID());
		inout.setMovementDate(lines.get(0).getUpdated());
		if (inout.isSOTrx())
			inout.setMovementType(MInOut.MOVEMENTTYPE_CustomerShipment);
		else
			inout.setMovementType(MInOut.MOVEMENTTYPE_VendorReceipts);
		inout.setAD_Org_ID(Env.getAD_Org_ID(Env.getCtx()));
		inout.setDocAction(DOCACTION_Prepare);
		inout.setC_DocType_ID();
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

		//do not process Replenish Movement generated WM InOut as it is directly Completed there
		if (getName().startsWith("Replenish")){ 
			setDocAction(DOCACTION_Close);
			return DocAction.STATUS_Completed;
		}
		
		MBPartner partner = (MBPartner) getC_BPartner();
		if (getName().endsWith("CONSIGNMENT")) {
			//create Movement, and update WM_EmptyStorage/Lines
			MWM_DeliverySchedule deliveryschedule = new Query(getCtx(),MWM_DeliverySchedule.Table_Name,MWM_DeliverySchedule.COLUMNNAME_WM_DeliverySchedule_ID+"=?",get_TrxName())
			.setParameters(getWM_DeliverySchedule_ID())
			.first();
			if (deliveryschedule==null)
				throw new AdempiereException("NO DeliverySchedule for:"+getName());
			  
			MMovement move = createConsignmentMovement(this);
			if (!move.processIt(MMovement.DOCACTION_Complete)) {
					throw new IllegalStateException("Movement Process Failed: " + move + " - " + move.getProcessMsg());
				}
		
			return "Consignment Movement Processed: "+partner.getName();
		}
		
		//Create Material Receipt process    
		MInOut inout = null;
		List<MWM_InOutLine> lines = new Query(Env.getCtx(),MWM_InOutLine.Table_Name,MWM_InOutLine.COLUMNNAME_WM_InOut_ID+"=?",get_TrxName())
				.setParameters(this.get_ID()).list();
		//holder for separate M_InOut according to different C_Order
		int c_Order_Holder = 0;
		for (MWM_InOutLine wioline:lines){
			MWM_DeliveryScheduleLine del = new Query(Env.getCtx(),MWM_DeliveryScheduleLine.Table_Name,MWM_DeliveryScheduleLine.COLUMNNAME_WM_DeliveryScheduleLine_ID+"=?",get_TrxName())
					.setParameters(wioline.getWM_DeliveryScheduleLine_ID())
					.first();
			if (del!=null && !del.isReceived())
				throw new AdempiereException("DeliverySchedule Line still not Received"); //still not processed at DeliverySchedule level, so no Shipment/Receipt possible
			if (wioline.getM_InOutLine_ID()>0)
				throw new AdempiereException("Already has Shipment/Receipt record!");//already done before
			if (del!=null && del.getC_OrderLine().getC_Order_ID()!=c_Order_Holder){
				if (inout!=null){
					saveM_InOut(inout,lines);
				}
				//create new MInOut  as C_Order_ID has changed
				inout = new MInOut(Env.getCtx(),0,get_TrxName());
				saveM_InOut(inout,lines);
				c_Order_Holder = del.getC_OrderLine().getC_Order_ID();
			}

			processWMSStorage(wioline,del,util);
			
			MInOutLine ioline = new MInOutLine(inout);
			ioline.setC_OrderLine_ID(wioline.getC_OrderLine_ID());
			ioline.setM_Product_ID(wioline.getM_Product_ID());
			ioline.setM_AttributeSetInstance_ID(wioline.getM_AttributeSetInstance_ID());
			ioline.setC_UOM_ID(wioline.getC_UOM_ID());
			ioline.setM_Locator_ID(wioline.getM_Locator_ID());
			ioline.setQtyEntered(wioline.getQtyPicked());
			ioline.setMovementQty(wioline.getQtyPicked());
			ioline.saveEx(get_TrxName());		
			//populate back WM_InOutLine with M_InOutLine_ID
			wioline.setM_InOutLine_ID(ioline.get_ID());ioline.getM_Locator();ioline.getM_Warehouse_ID();
			wioline.saveEx(get_TrxName());
			//if Sales' Shipment, then release the Handling Unit <--deprecated
			MWM_HandlingUnit hu = new Query(Env.getCtx(),MWM_HandlingUnit.Table_Name,MWM_HandlingUnit.COLUMNNAME_WM_HandlingUnit_ID+"=?",get_TrxName())
					.setParameters(wioline.getWM_HandlingUnit_ID())
					.first();
			if (hu!=null) {
				//deactivate HandlingUnit history
				MWM_HandlingUnitHistory huh = new Query(Env.getCtx(),MWM_HandlingUnitHistory.Table_Name,MWM_HandlingUnitHistory.COLUMNNAME_WM_HandlingUnit_ID+"=? AND "
						+MWM_HandlingUnitHistory.COLUMNNAME_WM_InOutLine_ID+"=?",get_TrxName())
						.setParameters(hu.get_ID(),wioline.get_ID())
						.first();
				if (huh==null){
						log.severe("HandlingUnit has no history: "+wioline.getWM_HandlingUnit().getName());
						continue;
				}
				if (huh.getDateEnd()==null){
					log.warning("HandlingUnit history has no DateEnd during Receive of DeliverySchedule: "+wioline.getWM_HandlingUnit().getName());
					huh.setDateEnd(hu.getUpdated());
				}
				huh.setIsActive(false);
				huh.saveEx(get_TrxName());
			}
			//check if has previous BackOrder that is not complete (no QtyDelivered value) so disallow any new BackOrders 
			//check if has previous WM_InOut (backorder case) and if QtyDelivered then error of premature process
			MWM_DeliveryScheduleLine prevDsLine = new Query(Env.getCtx(),MWM_DeliveryScheduleLine.Table_Name,MWM_DeliveryScheduleLine.COLUMNNAME_C_OrderLine_ID+"=?"
					+ " AND "+MWM_DeliveryScheduleLine.COLUMNNAME_IsBackOrder+"=? "
							+ "AND "+MWM_DeliveryScheduleLine.COLUMNNAME_Received+"=?"
									+ " AND "+MWM_DeliveryScheduleLine.COLUMNNAME_Created+"<?",get_TrxName())
					.setParameters(del.getC_OrderLine_ID(),"Y","Y",del.getCreated())
					.setOrderBy(COLUMNNAME_Created+ " DESC")
					.first(); 
 			
			//check if old backorder needs to reset
			//get C_Orderline, check if Delivered=Ordered
			MOrderLine orderline = new Query(Env.getCtx(),MOrderLine.Table_Name,MOrderLine.COLUMNNAME_C_OrderLine_ID+"=?",get_TrxName())
						.setParameters(del.getC_OrderLine_ID())
						.first();
			if (orderline!=null){
				//the prev DS Line backorder has to be updated by this new DS Line
				if (prevDsLine!=null){
					prevDsLine.setQtyDelivered(prevDsLine.getQtyDelivered().add(del.getQtyOrdered()));
					if (prevDsLine.getQtyDelivered().compareTo(prevDsLine.getQtyOrdered())==0) 
						prevDsLine.saveEx(get_TrxName());
				} 
			} 
		}
		if (inout!=null){
			saveM_InOut(inout,lines);		
			inout.setDescription(isSOTrx()?"Picking":"Putaway");
			inout.setDocStatus(this.DOCSTATUS_InProgress);
			inout.setDocAction(this.DOCACTION_Complete);
			inout.processIt(DocAction.ACTION_Complete);
		}
		//	Implicit Approval
		if (!isApproved())
			approveIt(); 
		
		if (log.isLoggable(Level.INFO)) log.info(toString());

		StringBuilder info = new StringBuilder();
		
		info.append("@M_InOut_ID@ - "+(isSOTrx()?"Shipment ":"Receipt ")+inout.getLines().length+" lines");
		
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
		return null;
	}

 	public String getDocumentNo() {
		return Msg.getElement(getCtx(), X_WM_InOut.COLUMNNAME_WM_InOut_ID) + " " + getDocumentNo();
 	}

 	public String getDocumentInfo() {
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
		return 0;
	}

 	public int getC_Currency_ID() {
		return 0;
	}

 	public BigDecimal getApprovalAmt() {
		return null;
	}
 	/**
 	 * WM_EmptyStorage is processed here to affect Vacant Storage Qty and Percentage Available
 	 * @param iolineID
 	 * @param dsline
 	 */
 	private void processWMSStorage(MWM_InOutLine wioline,MWM_DeliveryScheduleLine dsline,Utils util) {
		if (dsline!=null && !dsline.isReceived())
			throw new AdempiereException("DeliveryLine not Received. Complete its DeliverySchedule first.");
		else {
			if (wioline==null)
				throw new AdempiereException("WMS InOutLine lost!");
			BigDecimal eachQty=uomFactors(wioline, Env.ZERO);
			MWM_EmptyStorage storage = new Query(Env.getCtx(),MWM_EmptyStorage.Table_Name,MWM_EmptyStorage.COLUMNNAME_M_Locator_ID+"=?",get_TrxName())
					.setParameters(wioline.getM_Locator_ID())
					.first();wioline.getM_Product().getValue();
			if (wioline.getWM_InOut().isSOTrx()) { //OutBound confirmation
				BigDecimal picked = wioline.getQtyPicked().divide(boxConversion,2,RoundingMode.HALF_EVEN); 			
				BigDecimal vacancy = storage.getAvailableCapacity().add(picked); 
				storage.setAvailableCapacity(vacancy);
				MWM_EmptyStorageLine oldESLine = new Query(getCtx(), MWM_EmptyStorageLine.Table_Name, MWM_EmptyStorageLine.COLUMNNAME_M_Product_ID+"=? AND "
						+MWM_EmptyStorageLine.COLUMNNAME_WM_EmptyStorage_ID+"=?", get_TrxName())
						.setParameters(wioline.getM_Product_ID(),storage.get_ID())
						.first();
				util.pickedEmptyStorageLine(picked, oldESLine);
			}
			else { 	//purchasing InBound
				MProduct product = (MProduct)wioline.getM_Product();
				MWM_EmptyStorageLine newESLine = util.newEmptyStorageLine(dsline, wioline.getQtyPicked(), storage, wioline);
				if (product.getGuaranteeDays()>0)
					newESLine.setDateEnd(TimeUtil.addDays(wioline.getUpdated(), product.getGuaranteeDays()));	
				storage.setAvailableCapacity(storage.getAvailableCapacity().subtract(wioline.getQtyPicked().divide(boxConversion,2,RoundingMode.HALF_EVEN)));
				newESLine.setWM_HandlingUnit_ID(wioline.getWM_HandlingUnit_ID());
				newESLine.saveEx(get_TrxName());
			}
			if (dsline==null)
				util.calculatePercentageVacant(true,storage);
			else
				util.calculatePercentageVacant(dsline.isReceived(),storage);
		//TODO IsActive = N when DeliverySchedule.DocStatus='CO' and IsSOTrx 
			log.info("Processed InoutLine:"+wioline.toString());
		}
 	}
 	
	private BigDecimal uomFactors(MWM_InOutLine line, BigDecimal balance) {
		boxConversion=Env.ONE;
		BigDecimal picked = line.getQtyPicked();//.multiply(new BigDecimal(product.getUnitsPerPack()));

		//Current = current UOM Conversion Qty	
		MUOMConversion currentuomConversion = new Query(Env.getCtx(),MUOMConversion.Table_Name,MUOMConversion.COLUMNNAME_M_Product_ID+"=? AND "
				+MUOMConversion.COLUMNNAME_C_UOM_To_ID+"=?",null)
				.setParameters(line.getM_Product_ID(),line.getC_UOM_ID())
				.first();
		if (currentuomConversion!=null)
			currentUOM = currentuomConversion.getDivideRate();
		BigDecimal eachQty=picked.multiply(currentUOM);
		if (balance.compareTo(Env.ZERO)>0)
			eachQty=balance.multiply(currentUOM);

		//Pack Factor calculation
		MUOMConversion highestUOMConversion = new Query(Env.getCtx(),MUOMConversion.Table_Name,MUOMConversion.COLUMNNAME_M_Product_ID+"=?",null)
				.setParameters(line.getM_Product_ID())
				.setOrderBy(MUOMConversion.COLUMNNAME_DivideRate+" DESC")
				.first(); 
		if (highestUOMConversion!=null) {
			boxConversion = highestUOMConversion.getDivideRate();
			if (currentUOM==Env.ONE)
				return eachQty;
			if (currentuomConversion.getDivideRate().compareTo(highestUOMConversion.getDivideRate())!=0)//Plastic5 scenario
				packFactor = boxConversion.divide(currentUOM,2,RoundingMode.HALF_EVEN);
			else
				packFactor = boxConversion;
		}else
			boxConversion=picked;//avoid non existent of box type, making each line a box by default
		return eachQty;
		} 
}

