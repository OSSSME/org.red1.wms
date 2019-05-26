/**
* Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.
* If you shall try to cheat and find a loophole in this license, then KARMA will exact your share,
* and your worldly gain shall come to naught and those who share shall gain eventually above you.
* In compliance with previous GPLv2.0 works of Jorg Janke, Low Heng Sin, Carlos Ruiz and contributors.
* This Module Creator is an idea put together and coded by Redhuan D. Oon (red1@red1.org)
*/

package org.wms.process;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.List;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MMovement;
import org.compiere.model.MMovementLine;
import org.compiere.model.MOrder;
import org.compiere.model.MProduct;
import org.compiere.model.MWarehouse;
import org.compiere.model.Query;
import org.compiere.model.X_M_Locator;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.wms.model.MWM_DeliverySchedule;
import org.wms.model.MWM_DeliveryScheduleLine;
import org.wms.model.MWM_EmptyStorage;
import org.wms.model.MWM_EmptyStorageLine;
import org.wms.model.MWM_HandlingUnit;
import org.wms.model.MWM_HandlingUnitHistory;
import org.wms.model.MWM_InOut;
import org.wms.model.MWM_InOutLine;
import org.wms.model.MWM_StorageType;
	/**
	 * NOTE IN CONJUNCTION WITH CONSIGNMENT BPartner is both Vendor and Customer
	 * Documents generated are DeliverySchedule and WMS_InOut
	 * They are marked with DS.Name / WIO.Description = Consignment.Name + " CONSIGNMENT"
	 * M_Movement document is only created during WIO completion where DeliverySchedule isReceived.
	 * WM_EmptyStorage also updated during during WIO successful completion.
	 * HandlingUnit extract a Locator's content to another Locator's 
	 * Can move SameDistribution (add on to a HandlingUnit)
	 * Can move to Type of Locators instead of single.
	 * If No HU, then move all. If yes, partially by Qty or Percent to move.
	 * 
	 * @author red1
	 * @version 1.0
	 */
	public class StockMovement extends SvrProcess {

	private int WM_HandlingUnit_ID = 0; 
	private BigDecimal Percent = Env.ZERO; 
	private BigDecimal QtyMovement = Env.ZERO;  
	private int M_Locator_ID = 0; 
	private int WM_Type_ID = 0;  
	private int done=0;
	private boolean IsSameDistribution=false;
	private boolean IsSameLine=false;
	private boolean movement; 
	Timestamp now = new Timestamp (System.currentTimeMillis()); 
	private String trxName = "";
	private MBPartner partner = null;
	private int M_Warehouse_ID = 0;
	private boolean IsPutaway;
	private boolean IsPicking;
	
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
			for (ProcessInfoParameter p:para) {
				String name = p.getParameterName();
				if (p.getParameter() == null)
					;
				else if(name.equals("WM_HandlingUnit_ID")){
					WM_HandlingUnit_ID = p.getParameterAsInt();
			}
				else if(name.equals("Percent")){
					Percent = p.getParameterAsBigDecimal();
			}
				else if(name.equals("QtyMovement")){
					QtyMovement = p.getParameterAsBigDecimal();
			} 
				else if(name.equals("M_Locator_ID")){
					M_Locator_ID = p.getParameterAsInt();
			}	
				else if(name.equals("WM_Type_ID")){
				WM_Type_ID = p.getParameterAsInt();
			}	
				else if(name.equals("IsSameDistribution")){
				IsSameDistribution = p.getParameterAsBoolean();
			}
				else if(name.equals("IsPicking")){
				IsPicking = p.getParameterAsBoolean();
			}
				else if(name.equals("IsPutaway")){
				IsPutaway = p.getParameterAsBoolean();
			}
				else if(name.equals("IsSameLine")){
				IsSameLine = p.getParameterAsBoolean();
			} else if (name.equals("M_Warehouse_ID")) {
				M_Warehouse_ID =p.getParameterAsInt();
			}
		}
	}
	MWM_HandlingUnit hu = null;
	int storTypeCounter = 0;
	List<MWM_StorageType> stortypes = null;
	MWM_DeliverySchedule delivery = null; //init for Material Movement at end 
	String whereClause = "";
	List<MWM_EmptyStorageLine> selection = null;
	MWM_InOutLine ioline = null;
	MWM_DeliveryScheduleLine dline = null;
	MWM_EmptyStorage source = null;
	MProduct product = null;
	MWM_EmptyStorage target = null;
	BigDecimal balance = Env.ZERO;
	Utils util = null;
	private boolean checked=false;;
	
	private void setTrxName() {
		trxName = get_TrxName();
	}

	protected String doIt() {
		setTrxName();
		util = new Utils(trxName);
		util.setHandlingUnit(WM_HandlingUnit_ID);
		checkParams();
		//HandlingUnit to split the storage contents 
		selection = selectionFromInfoWindow();
		for (MWM_EmptyStorageLine line:selection){
			if (!checked) { 
				checkInOutIntegrity(line);
				checkDeliveryScheduleIntegrity(line);
			}
			//Product
			product = (MProduct) line.getM_Product();
			//source Storage
			source = (MWM_EmptyStorage)line.getWM_EmptyStorage();
			getPercentOrQty(line);
			//goto target to fit available pack Qty
			mainRoutine(line);
			done++;
		}
		if (movement)
			return "Lines done: "+done;
		else 
			return "Nothing Moved";
	}

	private void mainRoutine(MWM_EmptyStorageLine line) { 
		while (balance.compareTo(Env.ZERO)>0){
			if (WM_Type_ID>0)
				throw new AdempiereException("Type is not in use here"); //getStorageFromType(); 
			setTargetToLocator(); 
			BigDecimal alloted = allotQtyMovement(line);
			if (alloted.compareTo(balance)==0)
				continue;
			endSourceLine(line,alloted);
			createMovementSet(line,alloted);
		}
	}

	private void setTargetToLocator() {		
		//search by Warehouse
		if (M_Warehouse_ID>0) {
			MWarehouse warehouse = MWarehouse.get(getCtx(), M_Warehouse_ID);
			partner = new Query(Env.getCtx(),MBPartner.Table_Name,MBPartner.COLUMNNAME_Name+"=?",trxName)
					.setParameters(warehouse.getName())
					.first();
			X_M_Locator locator = new Query(Env.getCtx(),X_M_Locator.Table_Name,X_M_Locator.COLUMNNAME_M_Warehouse_ID+"=?",trxName)
					.setParameters(M_Warehouse_ID).first();
			target = new Query(Env.getCtx(),MWM_EmptyStorage.Table_Name,MWM_EmptyStorage.COLUMNNAME_M_Locator_ID+"=?",trxName)
					.setParameters(locator.get_ID()).first();
			M_Locator_ID = target.getM_Locator_ID();
		} else {
		target = new Query(Env.getCtx(),MWM_EmptyStorage.Table_Name,MWM_EmptyStorage.COLUMNNAME_M_Locator_ID+"=?",trxName)
				.setParameters(M_Locator_ID).first();
		}
		if (target==null)
			throw new AdempiereException("No EmptyStorage for Locator:"+M_Locator_ID);
	}

	private void checkParams() {
		if (WM_HandlingUnit_ID>0){
		} else {
			if ((Percent.add(QtyMovement)).compareTo(Env.ZERO)>0)
				throw new AdempiereException("Need Handling Unit if breaking up box content.");
		}
	}

	private List<MWM_EmptyStorageLine> selectionFromInfoWindow() {
		whereClause = "EXISTS (SELECT T_Selection_ID FROM T_Selection WHERE T_Selection.AD_PInstance_ID=? AND T_Selection.T_Selection_ID=WM_EmptyStorageLine.WM_EmptyStorageLine_ID)";
		List<MWM_EmptyStorageLine> lines = new Query(Env.getCtx(),MWM_EmptyStorageLine.Table_Name,whereClause,trxName)
		.setParameters(getAD_PInstance_ID())
		.list();
		return lines;
	}

	private void checkInOutIntegrity(MWM_EmptyStorageLine line) {
		ioline = new Query(Env.getCtx(),MWM_InOutLine.Table_Name,MWM_InOutLine.COLUMNNAME_WM_InOutLine_ID+"=?",trxName)
				.setParameters(line.getWM_InOutLine_ID())
				.first();
		if (ioline==null)
			throw new AdempiereException("StorageLine Movement has no WMS InOut record.");
		if (!ioline.getWM_InOut().getDocStatus().equals(MWM_InOut.DOCSTATUS_Completed))
			throw new AdempiereException("Stock was not properly putaway Complete before");
	}

	private void checkDeliveryScheduleIntegrity(MWM_EmptyStorageLine line) {
		dline = new Query(Env.getCtx(),MWM_DeliveryScheduleLine.Table_Name,MWM_DeliveryScheduleLine.COLUMNNAME_WM_DeliveryScheduleLine_ID+"=?",trxName)
				.setParameters(ioline.getWM_DeliveryScheduleLine_ID())
				.first();
		if (dline==null)
			throw new AdempiereException("StorageLine Movement does not have associated DeliveryLine");			
		if (!dline.isReceived()) {
			throw new AdempiereException("StorageLine is not Received in its DeliveryScheduleLine. Not done for: "+line);
		}
		checked=true;
	}

	private void getPercentOrQty(MWM_EmptyStorageLine line) {
		//define buffer as either QtyMovement or Percent value.		
		if (Percent.add(QtyMovement).compareTo(Env.ZERO)==0) {
			balance=line.getQtyMovement();
			return;
		}
		if (Percent.compareTo(Env.ZERO)>0){
			balance = line.getQtyMovement().divide(Env.ONEHUNDRED, 2,BigDecimal.ROUND_HALF_UP);
			balance = balance.multiply(Percent);
		} 
		if (balance.compareTo(line.getQtyMovement())>0)
			balance = line.getQtyMovement();
		else balance = QtyMovement;
	}

	/**
	 * Allot if available balance space at Target Storage
	 * @return
	 */
	private BigDecimal allotQtyMovement(MWM_EmptyStorageLine line) {  
		BigDecimal balanceFactor=balance.divide(new BigDecimal(product.getUnitsPerPack()),2,RoundingMode.HALF_EVEN);
		BigDecimal available = target.getAvailableCapacity(); 
		BigDecimal alloted = balance;
		if (available.compareTo(balanceFactor)<0)
			if (IsSameLine && WM_Type_ID==0 || WM_HandlingUnit_ID==0)
				throw new AdempiereException("Insufficient space at Target Storage for: "+balance+" X "+product.getName());
			else
				alloted=available.multiply(new BigDecimal(product.getUnitsPerPack()));
		if (IsSameLine && available.compareTo(balanceFactor)<0)
			return balance;
			 
		target.setAvailableCapacity(available.subtract(balanceFactor));	
		target.saveEx(trxName);		
		source.setAvailableCapacity(source.getAvailableCapacity().add(balanceFactor));
		source.saveEx(trxName);
		MWM_EmptyStorageLine eline = util.newEmptyStorageLine(dline, alloted, target, ioline);
		balance = balance.subtract(alloted);
		util.calculatePercentageVacant(dline.isReceived(), target);
		util.calculatePercentageVacant(dline.isReceived(), source);//TODO Available Capacity
		eline.saveEx(trxName);
		//update HU History
		if (WM_HandlingUnit_ID>0) {
			util.assignHandlingUnit(IsSameDistribution, ioline, eline, alloted);
			//update old Handling Unit history 
			MWM_HandlingUnitHistory ohuh = new Query(Env.getCtx(),MWM_HandlingUnitHistory.Table_Name,MWM_HandlingUnitHistory.COLUMNNAME_WM_HandlingUnit_ID+"=?",trxName)
					.setParameters(line.getWM_HandlingUnit_ID())
					.first();
			ohuh.setQtyMovement(ohuh.getQtyMovement().subtract(alloted));
			if (ohuh.getQtyMovement().compareTo(Env.ZERO)==0)
				ohuh.setDateEnd(source.getUpdated());
			ohuh.saveEx(trxName);
		}
		else
			;//Do Nothing further - change Locator only, retain same Handling Unit
		return alloted;
	}

	/**
	 * Counter incremented after each read TODO Not safe to use
	 * @param storTypeCounter
	 * @return
	 */
	private void getStorageFromType() {
		if (stortypes==null){
			stortypes = new Query(Env.getCtx(),MWM_StorageType.Table_Name,MWM_StorageType.COLUMNNAME_WM_Type_ID+"=?",trxName)
					.setParameters(WM_Type_ID).list();
			if (stortypes==null)
				throw new AdempiereException("WM_Type get StorageType fail");
		}
		M_Locator_ID = stortypes.get(storTypeCounter).getM_Locator_ID();
		 target = new Query(Env.getCtx(),MWM_EmptyStorage.Table_Name,MWM_EmptyStorage.COLUMNNAME_M_Locator_ID+"=?",trxName)
		.setParameters(M_Locator_ID)
		.first();
		storTypeCounter++;
	}

	private void endSourceLine(MWM_EmptyStorageLine line,BigDecimal alloted) {
		line.setQtyMovement(line.getQtyMovement().subtract(alloted));
		if (line.getQtyMovement().compareTo(Env.ZERO)==0){
			line.setDateEnd(source.getUpdated()); 
			line.setIsActive(false);
		}
		line.saveEx(trxName);
	}
	/*	This should be done from Cockpit BackOrder Management
	 * 	Create DS, WIO, Move docs
	 * 	WS marked as Received as WM InOut already generated to pick from locators
	 * 	
	 * 	related WM InOut set (implicit M_Movement when complete where its DS link has no Order)
	 *	moveline.setM_Locator_ID(ioline.getM_Locator_ID());
	 *	moveline.setM_LocatorTo_ID(M_Locator_ID);

	 */
	private void createMovementSet(MWM_EmptyStorageLine line,BigDecimal alloted) {
		//check if core M_InOut exist, then create a Material Movement record.
		MWM_InOut wio = null;
		MMovement move = null;
		String name = "Stock Movement TO "+target.getM_Locator().getValue()+" for "+(partner==null?"":partner.getName());
		if (movement==false){
			wio = new MWM_InOut(getCtx(),0,trxName);
			move = new MMovement(getCtx(),0,trxName);
			wio.setName(name); 
			move.setDescription(name);
			delivery = new MWM_DeliverySchedule(Env.getCtx(),0,get_TrxName());
			if(partner!=null) { 
				delivery.setC_BPartner_ID(partner.get_ID());
				delivery.setName(name);
				wio.setC_BPartner_ID(partner.get_ID());
				move.setC_BPartner_ID(partner.get_ID());
			}
			delivery.setDateDelivered(now);
			delivery.setC_DocType_ID(MDocType.getDocType(MDocType.DOCBASETYPE_MaterialMovement));
			delivery.saveEx(get_TrxName());
			
			//About MMovement ID to WM InOut .. workaround by matching Move.Description to WIO.Name.
			wio.saveEx(trxName);
			move.saveEx(trxName);
			movement=true;
			addBufferLog(wio.get_ID(), wio.getUpdated(), null,
					Msg.parseTranslation(getCtx(), "@WM_InOut_ID@ @Created@"),
					MWM_InOut.Table_ID, wio.get_ID());
		} 
		//create full set of DS, WIO lines from each source ESLine
			MWM_DeliveryScheduleLine deliveryline = new MWM_DeliveryScheduleLine(getCtx(),0,trxName);
			deliveryline.setWM_DeliverySchedule_ID(delivery.get_ID());
			deliveryline.setM_Product_ID(line.getM_Product_ID());
			deliveryline.setQtyDelivered(alloted);
			deliveryline.setReceived(true);
			deliveryline.saveEx(trxName);
			delivery.saveEx(trxName);
		 
			MWM_InOutLine wioline = new MWM_InOutLine(getCtx(), 0, trxName);
			wioline.setM_Product_ID(line.getM_Product_ID());
			wioline.setQtyPicked(alloted);
			wioline.setM_AttributeSetInstance_ID(ioline.getM_AttributeSetInstance_ID());
			wioline.setM_Locator_ID(M_Locator_ID);
	}
  
}
