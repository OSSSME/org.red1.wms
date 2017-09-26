/*** Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.* If you shall try to cheat and find a loophole in this license, then KARMA will exact your share,* and your worldly gain shall come to naught and those who share shall gain eventually above you.* In compliance with previous GPLv2.0 works of Jorg Janke, Low Heng Sin, Carlos Ruiz and contributors.* This Module Creator is an idea put together and coded by Redhuan D. Oon (red1@red1.org)*/package org.wms.component;
import java.math.BigDecimal;import org.adempiere.base.event.AbstractEventHandler;
import org.adempiere.base.event.IEventTopics;import org.adempiere.exceptions.AdempiereException;import org.compiere.model.MProduct;
import org.compiere.model.PO;import org.compiere.model.Query;
import org.compiere.util.CLogger;import org.compiere.util.Env;import org.compiere.util.TimeUtil;
import org.osgi.service.event.Event;import org.wms.model.MWM_DeliveryScheduleLine;import org.wms.model.MWM_EmptyStorage;import org.wms.model.MWM_EmptyStorageLine;import org.wms.model.MWM_InOutLine;import org.wms.process.Utils;

public class WM_DeliveryScheduleLineDocEvent extends AbstractEventHandler {
 	private static CLogger log = CLogger.getCLogger(WM_DeliveryScheduleLineDocEvent.class);
		private String trxName = "";
		private PO po = null;
		private Utils util = null;		
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
			log.info(" topic="+event.getTopic()+" po="+po);
			util = new Utils(trxName);						if (po instanceof MWM_DeliveryScheduleLine){
				if (IEventTopics.PO_AFTER_CHANGE == type){
					MWM_DeliveryScheduleLine dsline = (MWM_DeliveryScheduleLine)po;					if (dsline.isReceived()==(Boolean)dsline.get_ValueOld(MWM_DeliveryScheduleLine.COLUMNNAME_Received))						return;					if (dsline.isReceived()){						MWM_InOutLine ioline = new Query(Env.getCtx(),MWM_InOutLine.Table_Name,MWM_InOutLine.COLUMNNAME_WM_DeliveryScheduleLine_ID+"=?",trxName)							.setParameters(dsline.get_ID())							.first();						if (ioline==null)							return;												MWM_EmptyStorageLine esline = new Query(Env.getCtx(),MWM_EmptyStorageLine.Table_Name,MWM_EmptyStorageLine.COLUMNNAME_WM_InOutLine_ID+"=?",trxName)							.setParameters(ioline.get_ID())							.first(); 											if (esline==null)							throw new AdempiereException("Most likely No Handling Unit during Putaway. Solve it manually."); 												MWM_EmptyStorage storage = new Query(Env.getCtx(),MWM_EmptyStorage.Table_Name,MWM_EmptyStorage.COLUMNNAME_WM_EmptyStorage_ID+"=?",trxName)							.setParameters(esline.getWM_EmptyStorage_ID())							.first();											if (esline.isSOTrx()) { //OutBound confirmation							BigDecimal picked = esline.getQtyMovement();							BigDecimal picking = picked.multiply(new BigDecimal(esline.getM_Product().getUnitsPerPack()));										BigDecimal vacancy = storage.getAvailableCapacity().add(picking);							vacancy = vacancy.subtract(util.getFutureStorage(storage, dsline));							storage.setAvailableCapacity(vacancy);							util.pickedEmptyStorageLine(ioline, esline);						}						else { 	//purchasing InBound							MProduct product = (MProduct)dsline.getM_Product();							if (product.getGuaranteeDays()>0)								esline.setDateEnd(TimeUtil.addDays(dsline.getUpdated(), product.getGuaranteeDays()));								storage.setAvailableCapacity(storage.getAvailableCapacity().subtract(esline.getQtyMovement()));							BigDecimal PackFactor = new BigDecimal(product.getUnitsPerPack());							BigDecimal vacancy = storage.getAvailableCapacity().divide(PackFactor);									vacancy=vacancy.subtract(util.getFutureStorage(storage, dsline));							storage.setAvailableCapacity(vacancy.multiply(PackFactor));						}									util.calculatePercentageVacant(dsline,storage);						esline.saveEx(trxName);						//TODO IsActive = N when DeliverySchedule.DocStatus='CO' and IsSOTrx 						log.info("Delivery Received - InoutLine:"+ioline.toString()+" StorageLine:"+esline.toString());					}
					log.fine("MWM_DeliveryScheduleLine changed: "+dsline.get_ID());
				}}}}
	
	private void setPo(PO eventPO) {
		 po = eventPO;
	}

	private void setTrxName(String get_TrxName) {
		trxName = get_TrxName;
		}
}
