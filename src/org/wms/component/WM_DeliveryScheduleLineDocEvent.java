/*** Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.* If you shall try to cheat and find a loophole in this license, then KARMA will exact your share,* and your worldly gain shall come to naught and those who share shall gain eventually above you.* In compliance with previous GPLv2.0 works of Jorg Janke, Low Heng Sin, Carlos Ruiz and contributors.* This Module Creator is an idea put together and coded by Redhuan D. Oon (red1@red1.org)*/package org.wms.component;
import java.math.BigDecimal;import java.math.RoundingMode;import org.adempiere.base.event.AbstractEventHandler;
import org.adempiere.base.event.IEventTopics;import org.adempiere.exceptions.AdempiereException;import org.compiere.model.MOrderLine;import org.compiere.model.MProduct;import org.compiere.model.MUOMConversion;
import org.compiere.model.PO;import org.compiere.model.Query;
import org.compiere.util.CLogger;import org.compiere.util.Env;import org.compiere.util.TimeUtil;
import org.osgi.service.event.Event;import org.wms.model.MWM_DeliveryScheduleLine;import org.wms.model.MWM_EmptyStorage;import org.wms.model.MWM_EmptyStorageLine;import org.wms.model.MWM_InOutLine;import org.wms.process.Utils;

public class WM_DeliveryScheduleLineDocEvent extends AbstractEventHandler {
 	private static CLogger log = CLogger.getCLogger(WM_DeliveryScheduleLineDocEvent.class);
		private String trxName = "";
		private PO po = null;
		private Utils util = null;		private BigDecimal currentUOM=Env.ONE;		private BigDecimal packFactor=Env.ONE;		private BigDecimal eachQty = Env.ONE;		private BigDecimal boxConversion=Env.ONE;		
	@Override 
	protected void initialize() { 
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, MWM_DeliveryScheduleLine.Table_Name);
		log.info("WM_DeliveryScheduleLine<PLUGIN> .. IS NOW INITIALIZED");
		}

	@Override 
	protected void doHandleEvent(Event event){
		String type = event.getTopic();
		if (type.equals(IEventTopics.AFTER_LOGIN)) {
	}
 		else {
			setPo(getPO(event));
			setTrxName(po.get_TrxName());
			util = new Utils(trxName);						if (po instanceof MWM_DeliveryScheduleLine){
				if (IEventTopics.PO_AFTER_CHANGE == type){
					MWM_DeliveryScheduleLine dsline = (MWM_DeliveryScheduleLine)po;										if (dsline.isReceived()==(Boolean)dsline.get_ValueOld(MWM_DeliveryScheduleLine.COLUMNNAME_Received))						return;					if (dsline.isReceived()){						int iolineID = dsline.getWM_InOutLine_ID();//9Mac19/new Query(Env.getCtx(),MWM_InOutLine.Table_Name,MWM_InOutLine.COLUMNNAME_WM_DeliveryScheduleLine_ID+"=?",trxName).setParameters(dsline.get_ID()).first();						if (iolineID<1)							return;						eachQty=uomFactors(dsline);												MWM_EmptyStorageLine esline = new Query(Env.getCtx(),MWM_EmptyStorageLine.Table_Name,MWM_EmptyStorageLine.COLUMNNAME_WM_InOutLine_ID+"=?",trxName)							.setParameters(iolineID)							.first(); 											if (esline==null)							throw new AdempiereException("WMDeliveryScheduleLine PO_After_Change. WMEmptyStorageLine Not in WMInOutLine of DeliveryScheduleLine"); 												MWM_EmptyStorage storage = new Query(Env.getCtx(),MWM_EmptyStorage.Table_Name,MWM_EmptyStorage.COLUMNNAME_WM_EmptyStorage_ID+"=?",trxName)							.setParameters(esline.getWM_EmptyStorage_ID())							.first();						MWM_InOutLine ioline = new MWM_InOutLine(Env.getCtx(), iolineID, trxName);						if (esline.isSOTrx()) { //OutBound confirmation							BigDecimal picked = esline.getQtyMovement();							BigDecimal picking = picked.divide(boxConversion,2,RoundingMode.HALF_EVEN);										BigDecimal vacancy = storage.getAvailableCapacity().add(picking); 							storage.setAvailableCapacity(vacancy);							util.pickedEmptyStorageLine(ioline, esline);						}						else { 	//purchasing InBound							MProduct product = (MProduct)dsline.getM_Product();							if (product.getGuaranteeDays()>0)								esline.setDateEnd(TimeUtil.addDays(dsline.getUpdated(), product.getGuaranteeDays()));								storage.setAvailableCapacity(storage.getAvailableCapacity().subtract(esline.getQtyMovement().divide(boxConversion,2,RoundingMode.HALF_EVEN)));							BigDecimal vacancy = storage.getAvailableCapacity();	 							storage.setAvailableCapacity(vacancy);						}									util.calculatePercentageVacant(dsline.isReceived(),storage);//TODO Available Capacity						esline.saveEx(trxName);						//TODO IsActive = N when DeliverySchedule.DocStatus='CO' and IsSOTrx 						log.info("Delivery Received - InoutLine:"+ioline.toString()+" StorageLine:"+esline.toString());					}
					log.fine("MWM_DeliveryScheduleLine changed: "+dsline.get_ID());
				}}}}
		private BigDecimal uomFactors(MWM_DeliveryScheduleLine line) {		BigDecimal qtyEntered = line.getQtyOrdered();//.multiply(new BigDecimal(product.getUnitsPerPack()));		//Current = current UOM Conversion Qty		MUOMConversion currentuomConversion = new Query(Env.getCtx(),MUOMConversion.Table_Name,MUOMConversion.COLUMNNAME_M_Product_ID+"=? AND "		+MUOMConversion.COLUMNNAME_C_UOM_To_ID+"=?",null)				.setParameters(line.getM_Product_ID(),line.getC_UOM_ID())				.first();		if (currentuomConversion!=null)			currentUOM = currentuomConversion.getDivideRate();		BigDecimal eachQty=qtyEntered.multiply(currentUOM);				//Pack Factor calculation		MUOMConversion highestUOMConversion = new Query(Env.getCtx(),MUOMConversion.Table_Name,MUOMConversion.COLUMNNAME_M_Product_ID+"=?",null)				.setParameters(line.getM_Product_ID())				.setOrderBy(MUOMConversion.COLUMNNAME_DivideRate+" DESC")				.first(); 		if (highestUOMConversion!=null) {			boxConversion = highestUOMConversion.getDivideRate();			packFactor = boxConversion.multiply(highestUOMConversion.getDivideRate().divide(currentUOM,2,RoundingMode.HALF_EVEN));		}		return eachQty;	}	
	private void setPo(PO eventPO) {
		 po = eventPO;
	}

	private void setTrxName(String get_TrxName) {
		trxName = get_TrxName;
		}
}
