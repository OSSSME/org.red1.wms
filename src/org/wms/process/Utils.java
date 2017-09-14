package org.wms.process;

import java.math.BigDecimal;
import java.util.List;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MProduct;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.wms.model.MWM_DeliveryScheduleLine;
import org.wms.model.MWM_ESLine;
import org.wms.model.MWM_EmptyStorage;
import org.wms.model.MWM_EmptyStorageLine;
import org.wms.model.MWM_HandlingUnit;
import org.wms.model.MWM_HandlingUnitHistory;
import org.wms.model.MWM_InOut;
import org.wms.model.MWM_InOutLine;
import org.wms.model.X_WM_HandlingUnit;

public class Utils {
	public Utils(String processTrxName) {
		trxName = processTrxName; 
	}
	private String trxName="";
	private int WM_HandlingUnit_ID = 0;
	private boolean same = false;
	
	MWM_HandlingUnit hu = null;
	
	CLogger			log = CLogger.getCLogger (getClass());
	
	public void setHandlingUnit(int unit){
		WM_HandlingUnit_ID = unit;
	}
	
	/**
	 * SameDistribution = use same HandlingUnit
	 * @param inoutline
	 * @param empty
	 * @param qty
	 * @return WM_InOutLine
	 */
	public MWM_InOutLine assignHandlingUnit(boolean sameDistribution, MWM_InOutLine inoutline, MWM_EmptyStorageLine eline, BigDecimal qty) { 
		 if (sameDistribution && same){			 
		 }else{
				hu = new Query(Env.getCtx(),MWM_HandlingUnit.Table_Name,MWM_HandlingUnit.COLUMNNAME_WM_HandlingUnit_ID+">=? AND "
						+MWM_HandlingUnit.COLUMNNAME_QtyMovement+"=?",trxName)
						.setParameters(WM_HandlingUnit_ID,Env.ZERO)
						.setOrderBy(X_WM_HandlingUnit.COLUMNNAME_Name)
						.first();
				if (hu==null){
					log.severe("No Available Handling Unit starting from: "+WM_HandlingUnit_ID);
					return null;
				}
		 }
		 //SameDistribution = use same HandlingUnit for all selected items
		if (same){
			hu.setQtyMovement(hu.getQtyMovement().add(qty));
		}else {
			if (inoutline.getWM_InOut().isSOTrx()) //so no other picking can touch this
				hu.setDocStatus(X_WM_HandlingUnit.DOCSTATUS_InProgress);
			else 
				hu.setDocStatus(X_WM_HandlingUnit.DOCSTATUS_Completed);
			hu.setQtyMovement(qty); 
			hu.setM_Product_ID(inoutline.getM_Product_ID());
			WM_HandlingUnit_ID = hu.get_ID();
			if (sameDistribution)
				same = true;
		}
		hu.saveEx(trxName);
		
		//create new history
		MWM_HandlingUnitHistory huh = new MWM_HandlingUnitHistory(Env.getCtx(),0,trxName);
		huh.setWM_HandlingUnit_ID(WM_HandlingUnit_ID);
		huh.setWM_InOutLine_ID(inoutline.get_ID());
		huh.setC_Order_ID(inoutline.getC_OrderLine().getC_Order_ID());
		huh.setQtyMovement(qty);
		huh.setC_UOM_ID(inoutline.getC_UOM_ID());
		huh.setM_Product_ID(inoutline.getM_Product_ID());
		huh.setDateStart(hu.getUpdated());
		huh.saveEx(trxName);
		if (eline==null)
			log.warning("Assign Handling Unit - No EmptyStorageLine yet. InOutLine details: "+inoutline.toString());
		else {
			eline.setWM_HandlingUnit_ID(WM_HandlingUnit_ID);
			eline.saveEx(trxName);
		}
		inoutline.setWM_HandlingUnit_ID(WM_HandlingUnit_ID);
		inoutline.saveEx(trxName);
		return inoutline;
	}

	/**
	 * DocStatus = Drafted
	 * QtyMovement = ZERO
	 */
	public void releaseHandlingUnit(MWM_EmptyStorageLine line) { 
		MWM_HandlingUnit hu = new Query(Env.getCtx(),MWM_HandlingUnit.Table_Name,MWM_HandlingUnit.COLUMNNAME_WM_HandlingUnit_ID+"=?",trxName)
				.setParameters(line.getWM_HandlingUnit_ID()).first();
		hu.setQtyMovement(Env.ZERO);
		hu.setDocStatus(X_WM_HandlingUnit.DOCSTATUS_Drafted);
		hu.saveEx(trxName);
	}
	
	/**
	 * Handling Unit will be fully released during CompleteIt() of WM_InOut > M_InOut (IsSOTrx='Y')
	 * @param oline
	 * @param empty
	 */
	public void releaseHandlingUnitHistory(MWM_DeliveryScheduleLine dsline,MWM_InOutLine oline, MWM_EmptyStorageLine empty) {
		MWM_HandlingUnit hu = (MWM_HandlingUnit) empty.getWM_HandlingUnit();
		if (hu==null){
			log.severe("HandlingUnit not found for EmptyLine at this Locator - "+empty.getWM_EmptyStorage().getM_Locator().getValue());
			return;
		} 
		MWM_HandlingUnitHistory huh = new Query(Env.getCtx(),MWM_HandlingUnitHistory.Table_Name,MWM_HandlingUnitHistory.COLUMNNAME_WM_HandlingUnit_ID+"=? AND "
		+MWM_HandlingUnitHistory.COLUMNNAME_DateEnd+" IS NULL",trxName)
				.setParameters(hu.get_ID()).first();
		if (huh==null) {
			log.severe("No Handling Unit to release (DateEnd Not null)");
			return;
		}
		huh.setDateEnd(dsline.isReceived()?oline.getUpdated():dsline.getWM_DeliverySchedule().getDatePromised());	
		huh.saveEx(trxName); 
	}

	/**
	 * 
	 * @param dsline
	 * @param alloted
	 * @param empty
	 * @param inoutline
	 * @return WM_EmptyStorageLine
	 */
	public MWM_EmptyStorageLine newEmptyStorageLine(MWM_DeliveryScheduleLine dsline, BigDecimal alloted, MWM_EmptyStorage empty, MWM_InOutLine inoutline) {
		MWM_EmptyStorageLine storline = new MWM_EmptyStorageLine(Env.getCtx(),0,trxName);
		storline.setWM_EmptyStorage_ID(empty.get_ID());
		storline.setWM_InOutLine_ID(inoutline.getWM_InOutLine_ID());
		storline.setQtyMovement(alloted);
		storline.setIsSOTrx(dsline.getWM_DeliverySchedule().isSOTrx());
		if (dsline.isReceived())
			storline.setDateStart(dsline.getWM_DeliverySchedule().getDateDelivered());
		else
			storline.setDateStart(dsline.getWM_DeliverySchedule().getDatePromised());
		
		MProduct product = (MProduct)dsline.getM_Product();
		if (product.getGuaranteeDays()>0)
			storline.setDateEnd(TimeUtil.addDays(storline.getUpdated(), product.getGuaranteeDays()));
		
		storline.setC_UOM_ID(inoutline.getC_UOM_ID());
		storline.setM_Product_ID(inoutline.getM_Product_ID());
		storline.saveEx(trxName); 
		return storline;
	}

	/**
	 * 
	 * @param eline
	 * @param newline
	 * @return WM_ESLine
	 */
	public void createESLinePicking(MWM_EmptyStorageLine eline, MWM_EmptyStorageLine newline) {
		//link source SEL and new SEL with WM_ESLine
		MWM_ESLine link = new MWM_ESLine(Env.getCtx(),0,trxName);
		link.setWM_EmptyStorageLine_ID(eline.get_ID());
		link.setValue(new Integer(newline.get_ID()).toString());
		link.setProcessed(false);
		link.saveEx(trxName);
	} 
	
	/**
	 * Here it is finally picked, so the sub ESLine link is marked processed.
	 * Check if QtyMovement=0, then Set ELINE IsActive=false during CompleteIt() of Shipment
	 * @param eline
	 * @param inoutline
	 * @param newline
	 */
	public void pickedEmptyStorageLine(MWM_InOutLine inoutline,MWM_EmptyStorageLine newline) {  
		MWM_ESLine link = new Query(Env.getCtx(),MWM_ESLine.Table_Name,MWM_ESLine.COLUMNNAME_Value+"=?",trxName)
				.setParameters(new Integer(newline.get_ID()).toString()).first();
		if (link!=null){
			if (link.isProcessed()){
				log.info("ESLine link already processed ");
				return;
			}
			MWM_EmptyStorageLine eline =  new Query(Env.getCtx(),MWM_EmptyStorageLine.Table_Name,MWM_EmptyStorageLine.COLUMNNAME_WM_EmptyStorageLine_ID+"=?",trxName)
					.setParameters(link.getWM_EmptyStorageLine_ID()).first();
			MWM_EmptyStorageLine linkedline = new Query(Env.getCtx(),MWM_EmptyStorageLine.Table_Name,MWM_EmptyStorageLine.COLUMNNAME_WM_EmptyStorageLine_ID+"=?",trxName)
					.setParameters(new Integer(link.getValue()).intValue()).first();
			linkedline.setDateEnd(inoutline.getUpdated());
			linkedline.saveEx(trxName);	
			eline.setQtyMovement(eline.getQtyMovement().subtract(linkedline.getQtyMovement()));
			eline.saveEx(trxName);
			link.setProcessed(true);
			link.saveEx(trxName);
		}
	}
	
	/**
	 * 
	 * @param empty
	 * @param dline
	 * @return future forecast
	 */
	public BigDecimal getFutureStorage(MWM_EmptyStorage empty, MWM_DeliveryScheduleLine dline){
		BigDecimal forecastStorage =Env.ZERO;
		if (dline.isReceived())
			return Env.ZERO;
		List<MWM_EmptyStorageLine> slines = new Query(Env.getCtx(),MWM_EmptyStorageLine.Table_Name,MWM_EmptyStorageLine.COLUMNNAME_DateStart+"<=? AND "
				+MWM_EmptyStorageLine.COLUMNNAME_WM_EmptyStorage_ID+"=?",trxName)
				.setParameters(dline.getWM_DeliverySchedule().getDatePromised(),empty.get_ID())
				.setOnlyActiveRecords(true)
				.list();
		for (MWM_EmptyStorageLine sline:slines){
			if (sline.getDateEnd()!=null && sline.getDateEnd().before(dline.getWM_DeliverySchedule().getDatePromised()))
				continue;
			if (sline.isSOTrx())
				forecastStorage=forecastStorage.subtract(sline.getQtyMovement());
			else 
				forecastStorage=forecastStorage.add(sline.getQtyMovement());
		}
		return forecastStorage;
	}
	
	/**
	 * 
	 * @param dsline
	 * @param empty
	 */
	public void calculatePercentageVacant(MWM_DeliveryScheduleLine dsline,MWM_EmptyStorage empty) {
		if (!dsline.isReceived())
			return;//future, do not want to affect statistics of EmptyStorage
		empty.setPercentage((empty.getAvailableCapacity().divide(empty.getVacantCapacity(),4,BigDecimal.ROUND_HALF_UP)).multiply(Env.ONEHUNDRED));
		//set is Full if 0% vacant
		if (empty.getPercentage().compareTo(Env.ZERO)==0)
			empty.setIsFull(true);
		else
			empty.setIsFull(false);
		empty.saveEx(trxName);
	}
	
	/**
	 * 
	 * @param inout
	 * @param dsline
	 * @param alloted
	 * @return WM_InOutLine
	 */
	public MWM_InOutLine newInOutLine(MWM_InOut inout, MWM_DeliveryScheduleLine dsline, BigDecimal alloted) {
		MWM_InOutLine inoutline = new MWM_InOutLine(Env.getCtx(),0,trxName);
		inoutline.setWM_InOut_ID(inout.get_ID());
		inoutline.setC_UOM_ID(dsline.getC_UOM_ID());
		inoutline.setC_OrderLine_ID(dsline.getC_OrderLine_ID());
		inoutline.setM_Product_ID(dsline.getM_Product_ID());
		inoutline.setQtyPicked(alloted);
		inoutline.setWM_DeliveryScheduleLine_ID(dsline.get_ID());
		inoutline.saveEx(trxName);
		dsline.setWM_InOutLine_ID(inoutline.get_ID());
		dsline.saveEx(trxName);
		return inoutline;
	}
	
	/**
	 * 
	 * @param inout
	 */
	public void sortFinalList(MWM_InOut inout) {
		//sort inout list according to XYZ
		List<MWM_InOutLine>iolines = new Query(Env.getCtx(),MWM_InOutLine.Table_Name,MWM_InOutLine.COLUMNNAME_WM_InOut_ID+"=?",trxName)
				.setParameters(inout.get_ID())
				.setOrderBy(MWM_InOutLine.COLUMNNAME_M_Locator_ID)
				.list();
		int seq = 1;
		for (MWM_InOutLine line:iolines){	
			line.setSequence(new BigDecimal(seq));
			line.saveEx(trxName);
			seq++;			
		}
	}

	/**
	 * From WM_InOut Complete after creation of Shipment
	 * @param lines
	 */
	public void closeOutbound(List<MWM_InOutLine> lines) {
		if (lines==null)
			throw new AdempiereException("Suddenly WM_InOutLine(s) disappear!");
		for (MWM_InOutLine line:lines){
			MWM_DeliveryScheduleLine dsline = (MWM_DeliveryScheduleLine) line.getWM_DeliveryScheduleLine();
			MWM_EmptyStorageLine esline = new Query(Env.getCtx(),MWM_EmptyStorageLine.Table_Name,MWM_EmptyStorageLine.COLUMNNAME_WM_InOutLine_ID+"=?",trxName)
					.setParameters(line.getWM_InOutLine_ID()).first(); 
			MWM_HandlingUnit hu = (MWM_HandlingUnit) line.getWM_HandlingUnit();
			WM_HandlingUnit_ID = hu.get_ID(); 
			releaseHandlingUnitHistory(dsline, line, esline);
			releaseHandlingUnit(esline);
		}
	}
}
