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
import org.compiere.model.MProduct;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;
import org.wms.model.MWM_DeliverySchedule;
import org.wms.model.MWM_DeliveryScheduleLine;
import org.wms.model.MWM_EmptyStorage;
import org.wms.model.MWM_EmptyStorageLine;
import org.wms.model.MWM_HandlingUnit;
import org.wms.model.MWM_InOut;
import org.wms.model.MWM_InOutLine;
import org.wms.model.MWM_PreferredProduct;
import org.wms.model.MWM_ProductType;
import org.wms.model.MWM_StorageType;
import org.wms.model.MWM_WarehousePick;

/**
 * Create PutAway and Picking List for Warehouse Locators
 * InBound and OutBound routines to locate according to best practice WMS rules
 * Please refer to http://red1.org/adempiere/ forum
 * @author red1
 * @version 0.9.2
 */
	public class CreatePutawayList extends SvrProcess {

	private int M_Warehouse_ID = 0; 
	private int WM_HandlingUnit_ID = 0; 
	private int WM_InOut_ID = 0;
	private boolean IsSameDistribution = false;
	private boolean IsSameLine = true;
	private String RouteOrder = ""; //normal
	private String X = "Z"; 
	private String Y = "Z";
	private String Z = "Z";
	private int putaways;
	private int pickings;
	private int notReceived=0;
	private boolean isReceived=false;
	private boolean isSOTrx;
	Timestamp now = new Timestamp (System.currentTimeMillis()); 
	Utils util = null;
	private String trxName = "";
	private boolean external = false;
	MWM_DeliverySchedule externalDeliverySchedule  = null;
	
	public CreatePutawayList(){
		
	}
	
	public CreatePutawayList(MWM_DeliverySchedule schedule, int wM_HandlingUnit_ID2, boolean sameline, boolean samedistriution) { 
		WM_HandlingUnit_ID=wM_HandlingUnit_ID2;
		setTrxName(schedule.get_TrxName());
		externalDeliverySchedule = schedule;
		IsSameLine=sameline;
		IsSameDistribution=samedistriution;
		external = true;
	}
	private void setTrxName(String get_TrxName) {
		trxName = get_TrxName;		
	}
	public String executeDoIt(){
		return doIt();
	}
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
			for (ProcessInfoParameter p:para) {
				String name = p.getParameterName();
				if (p.getParameter() == null)
					;
				else if(name.equals("WM_HandlingUnit_ID")){
					WM_HandlingUnit_ID = p.getParameterAsInt();
				}		
				else if(name.equals("IsSameLine")){
					IsSameLine = (Boolean)p.getParameterAsBoolean();
				}
				else if(name.equals("M_Warehouse_ID")){
				M_Warehouse_ID = p.getParameterAsInt();
				}
				else if(name.equals("IsSameDistribution")){
					IsSameDistribution = "Y".equals(p.getParameter());
				}
				else if(name.equals("X")){
					X = (String)p.getParameter();
				}
				else if(name.equals("Y")){
					Y = (String)p.getParameter();
				}
				else if(name.equals("Z")){
					Z = (String)p.getParameter();
				}		
				else if(name.equals("RouteOrder")){
					RouteOrder = p.getParameterAsString();
				}	
				else if(name.equals("WM_InOut_ID")){
					WM_InOut_ID = p.getParameterAsInt();
				}	
			}
			setTrxName(get_TrxName());
		}
	/**
	 * Main routine 
	 * Determine Lines exists
	 * Create new WM_InOut header.
	 * Segregate between InBound and OutBound process
	 */
	protected String doIt() {
		String whereClause = "EXISTS (SELECT T_Selection_ID FROM T_Selection WHERE T_Selection.AD_PInstance_ID=? AND T_Selection.T_Selection_ID=WM_DeliveryScheduleLine.WM_DeliveryScheduleLine_ID)";
		List<MWM_DeliveryScheduleLine> lines = null;
		
		if (external){
			lines = new Query(Env.getCtx(),MWM_DeliveryScheduleLine.Table_Name,MWM_DeliveryScheduleLine.COLUMNNAME_WM_DeliverySchedule_ID+"=?",trxName)
					.setParameters(externalDeliverySchedule.get_ID()).list();			
		}else {
			lines = new Query(Env.getCtx(),MWM_DeliveryScheduleLine.Table_Name,whereClause,trxName)
					.setParameters(getAD_PInstance_ID())
					.list();				
		}
	
		if (lines==null || lines.isEmpty())
			return "No Lines";
		
		util = new Utils(trxName);
		util.setHandlingUnit(WM_HandlingUnit_ID);
		
		MWM_InOut inout = null;
		if (WM_InOut_ID>0) {
			inout = new Query(Env.getCtx(), MWM_InOut.Table_Name, MWM_InOut.COLUMNNAME_WM_InOut_ID+"=?", trxName)
					.setParameters(WM_InOut_ID)
					.first();
		} else {
			inout = new MWM_InOut(Env.getCtx(),0,trxName);
			inout.setC_BPartner_ID(lines.get(0).getWM_DeliverySchedule().getC_BPartner_ID());
			inout.setWM_DeliverySchedule_ID(lines.get(0).getWM_DeliverySchedule_ID());
			inout.setName(lines.get(0).getWM_DeliverySchedule().getName());
			inout.setIsSOTrx(lines.get(0).getWM_DeliverySchedule().isSOTrx());
			inout.setWM_Gate_ID(lines.get(0).getWM_DeliverySchedule().getWM_Gate_ID());
		}

		inout.saveEx(trxName);
		putaways = 0;
		pickings = 0;
		
		isSOTrx = inout.isSOTrx();
		
		if (isSOTrx){
			pickingProcess(inout,lines);
			util.sortFinalList(inout);
			return "Successful Pickings: "+pickings+" (Future: "+notReceived+")";
		} else{
			putawayProcess(inout,lines);
			util.sortFinalList(inout);
			return "Successful Putaways: "+putaways+" (Future: "+notReceived+")";
		}
		 
	}

	private void putawayProcess(MWM_InOut inout, List<MWM_DeliveryScheduleLine> lines) {
		for (MWM_DeliveryScheduleLine line:lines){
			
			if (line.getWM_InOutLine_ID()>0)
				continue;//already done
			
			if (!line.isReceived()){
				notReceived++;
				isReceived=false;
			} else
				isReceived=true;
			
			//running balance in use thru-out here
			BigDecimal balance =line.getQtyDelivered();				
			
			//get Product from InOut Bound line
			MProduct product = (MProduct) line.getM_Product();
			
			//If No Handling Unit required at this juncture, 
			//then no M_Locator putAway also. Manual way.
			if (WM_HandlingUnit_ID<1 && !isSOTrx) {
				throw new AdempiereException("Please select Handling Unit. It is for background processing.");
				//Do not allow as yet due to user confusion its not putaway.
				//util.newInOutLine(inout,line,balance);
				//continue;// avoid Locator and EmptyStorage
			}

			//check if defined in PreferredProduct...
			List<MWM_PreferredProduct> preferreds = new Query(Env.getCtx(),MWM_PreferredProduct.Table_Name,MWM_PreferredProduct.COLUMNNAME_M_Product_ID+"=?" ,trxName)
					.setParameters(product.get_ID())
					.setOrderBy(MWM_PreferredProduct.COLUMNNAME_M_Locator_ID)
					.list();
			boolean done=false;
			if (preferreds!=null){
				for (MWM_PreferredProduct preferred:preferreds){
					 
					if (M_Warehouse_ID>0){
						if (preferred.getM_Locator().getM_Warehouse_ID()!=M_Warehouse_ID)
							continue; 
					}
					if (preferred.getM_Locator().getX().compareTo(X)>=0 || preferred.getM_Locator().getY().compareTo(Y)>=0  || preferred.getM_Locator().getZ().compareTo(Z)>=0 )
						continue;
					//get next EmptyStorage, if fit, then break, otherwise if balance, then continue
					int locator_id = preferred.getM_Locator_ID();
					balance = startPutAwayProcess(inout,line,balance,locator_id);
					if (balance.compareTo(Env.ZERO)>0)
						continue;
					else {
						done=true;
						break;
					}
				}
			} 
			if (done)
				continue; //done so go to next DeliveryScheduleLine. 
			
			//get ProductType = StorageType
			MWM_ProductType prodtype = new Query(Env.getCtx(),MWM_ProductType.Table_Name,MWM_ProductType.COLUMNNAME_M_Product_ID+"=?",trxName)
					.setParameters(product.get_ID())
					.first();
			 	if (prodtype!=null){
					List<MWM_StorageType> stortypes= new Query(Env.getCtx(),MWM_StorageType.Table_Name,MWM_StorageType.COLUMNNAME_WM_Type_ID+"=?",trxName)
							.setParameters(prodtype.getWM_Type_ID())
							.setOrderBy(MWM_StorageType.COLUMNNAME_WM_Type_ID)
							.list();				
					for (MWM_StorageType stortype:stortypes){
						
						if (stortype!=null){
							if (stortype.getM_Locator().getX().compareTo(X)>=0 || stortype.getM_Locator().getY().compareTo(Y)>=0  || stortype.getM_Locator().getZ().compareTo(Z)>=0 )
								continue;
							if (M_Warehouse_ID>0)
								if (stortype.getM_Locator().getM_Warehouse_ID()!=M_Warehouse_ID)
									continue;
							
							//get next EmptyStorage, if fit, then break, otherwise if balance, then continue
							int locator_id = stortype.getM_Locator_ID(); 
							balance = startPutAwayProcess(inout,line,balance,locator_id);
							if (balance.compareTo(Env.ZERO)>0)
								continue;
							else {
								done=true;
								break;
							}
						}	
					}
			 	}
				
			 if (done)
					continue; //enough, i already putaway all.
			//get non reserved empty storage
			List<MWM_EmptyStorage> empties = new Query(Env.getCtx(),MWM_EmptyStorage.Table_Name,MWM_EmptyStorage.COLUMNNAME_IsFull+"=? AND "
					+MWM_EmptyStorage.COLUMNNAME_IsFull+"=?",trxName)
				.setParameters(false,false)
				.setOrderBy(MWM_EmptyStorage.COLUMNNAME_M_Locator_ID)
				.list();	
			
			if (empties==null)
				throw new AdempiereException("NO MORE EMPTY STORAGE");
			
			for (MWM_EmptyStorage empty:empties){
				if (M_Warehouse_ID>0)
					if (empty.getM_Locator().getM_Warehouse_ID()!=M_Warehouse_ID)
						continue;
				if (empty.getM_Locator().getX().compareTo(X)>=0 || empty.getM_Locator().getY().compareTo(Y)>=0  || empty.getM_Locator().getZ().compareTo(Z)>=0 )
						continue;

				//if has StorType then continue also
				MWM_StorageType storagetype = new Query(Env.getCtx(),MWM_StorageType.Table_Name,MWM_StorageType.COLUMNNAME_M_Locator_ID+"=?",trxName)
					.setParameters(empty.getM_Locator_ID())
					.first();
				if (storagetype!=null)
					continue;
			
				//if has PreferredProduct then continue also
				MWM_PreferredProduct preferred = new Query(Env.getCtx(),MWM_StorageType.Table_Name,MWM_StorageType.COLUMNNAME_M_Locator_ID+"=?",trxName)
					.setParameters(empty.getM_Locator_ID())
					.first();
				if (preferred!=null)
					continue;
			
				//get next EmptyStorage, if fit, then break, otherwise if balance, then continue
				int locator_id = empty.getM_Locator_ID();
				balance = startPutAwayProcess(inout,line,balance,locator_id);
				if (balance.compareTo(Env.ZERO)>0)
					continue;
				else {
					break;
				}
			} 
			if (balance.compareTo(Env.ZERO)>0)
				log.warning("NO Storage Found for "+product.getName());
		}
	}

	/**
	 * 
	 * @param balance
	 * @param locator_id
	 * @return balance of unallocated qty to empty storage
	 */
	private BigDecimal startPutAwayProcess(MWM_InOut inout, MWM_DeliveryScheduleLine dsline, BigDecimal balance, int locator_id) {
		MWM_EmptyStorage empty = new Query(Env.getCtx(),MWM_EmptyStorage.Table_Name,MWM_EmptyStorage.COLUMNNAME_M_Locator_ID+"=?",trxName)
				.setParameters(locator_id)
				.first();
		if (empty==null)
			throw new AdempiereException("No Empty Storage set for locator id: "+locator_id);
		
		//if its full go back and look for next EmptyStorage
		if (empty.isFull())
			return balance;
	
		//allotted holds for underlying EmtpyStorageLine detail
		BigDecimal alloted = balance;
		//check if its vacant capacity can handle the balance.
		//pack factor is multiplied into VacantCapacity denominator.
		MProduct product = (MProduct) dsline.getM_Product();
		BigDecimal PackFactor = new BigDecimal(product.getUnitsPerPack());
		 
		BigDecimal vacancy = empty.getAvailableCapacity().multiply(PackFactor);		
		vacancy=vacancy.subtract(util.getFutureStorage(empty, dsline));
		
		if (balance.compareTo(vacancy)>=0 && IsSameLine==false){
			if (isReceived)
				empty.setAvailableCapacity(Env.ZERO);
			balance = balance.subtract(vacancy);	
			alloted = vacancy;
		} else {
			vacancy = vacancy.subtract(balance);
			if (vacancy.compareTo(Env.ZERO)<0)//available insufficient
				return balance;
			if (isReceived)
				empty.setAvailableCapacity(vacancy.divide(PackFactor,2,RoundingMode.HALF_EVEN));
			alloted = balance;
			balance = Env.ZERO;
		}  
		MWM_InOutLine inoutline = util.newInOutLine(inout,dsline,alloted); 
		setLocator(inoutline,locator_id); 
		MWM_EmptyStorageLine eline = util.newEmptyStorageLine(dsline,alloted,empty,inoutline);
		if (isReceived)
			inoutline = util.assignHandlingUnit(IsSameDistribution,inoutline,eline,alloted); 
		if (isReceived)
			util.calculatePercentageVacant(dsline.isReceived(),empty);// 
		return balance;
	}

	private void setLocator(MWM_InOutLine line, int put_pick) { 
		line.setM_Locator_ID(put_pick);
		line.saveEx(trxName);
		putaways++;
	}

	private void pickingProcess(MWM_InOut inout, List<MWM_DeliveryScheduleLine> lines) {
		for (MWM_DeliveryScheduleLine line:lines){
			
			if (line.getWM_InOutLine_ID()>0)
				continue;//already done
			
			if (!line.isReceived()){
				notReceived++;
				isReceived=false;
			} else
				isReceived=true;
			
			//running balance in use thru-out here
			BigDecimal balance =line.getQtyDelivered();			
			
			//If No Handling Unit required at this juncture, then no M_Locator putAway also. Manual way.
			if (WM_HandlingUnit_ID<1 && !isSOTrx) {
				util.newInOutLine(inout,line,balance);
				continue;// avoid Locator and EmptyStorage
			}
			//if Handling Unit is set, then assign while creating WM_InOuts. EmptyLocators also assigned. Can be cleared and reassigned in next Info-Window
			if (!getPickingLocators(inout,line))
				log.warning("Check Log for any SEVERE messages. Could not finish picking: "+line.getQtyOrdered()+" "+line.getM_Product().getName());
		}	
	}

	private boolean getPickingLocators(MWM_InOut inout,MWM_DeliveryScheduleLine line) {  
		MProduct product = (MProduct)line.getM_Product();
		if (product==null) {
			log.severe("Fatal: Suddenly Delivery Line has no Product!");
			return false;
		}
		//check if there was WM_WarehousePick (for preselected pick during Sales Order)
		if (line.getC_OrderLine_ID()>0){line.getM_Product().getName();
			if  (orderLineWarehousePick(inout, line))
				return true; 
		}
		
		//NOrmal (shortest), FIfo, or LIfo based on previous putaway date start order
		List<MWM_EmptyStorageLine>elines = null;
		if (RouteOrder.equals("NO")) {
			elines = new Query(Env.getCtx(),MWM_EmptyStorageLine.Table_Name,MWM_EmptyStorageLine.COLUMNNAME_M_Product_ID+"=? AND "+MWM_EmptyStorageLine.COLUMNNAME_QtyMovement+">? AND ISSOTRX=?",trxName)
					.setParameters(product.get_ID(),0,false)
					.setOrderBy(MWM_EmptyStorageLine.COLUMNNAME_WM_EmptyStorage_ID)
					.list();
		}else if (RouteOrder.equals("FI")) {
			elines = new Query(Env.getCtx(),MWM_EmptyStorageLine.Table_Name,MWM_EmptyStorageLine.COLUMNNAME_M_Product_ID+"=? AND "+MWM_EmptyStorageLine.COLUMNNAME_QtyMovement+">? AND ISSOTRX=?",trxName)
					.setParameters(product.get_ID(),0,false)
					.setOrderBy(MWM_EmptyStorageLine.COLUMNNAME_DateStart)
					.list();
		}else if (RouteOrder.equals("LI")) {
			elines = new Query(Env.getCtx(),MWM_EmptyStorageLine.Table_Name,MWM_EmptyStorageLine.COLUMNNAME_M_Product_ID+"=? AND "+MWM_EmptyStorageLine.COLUMNNAME_QtyMovement+">? AND ISSOTRX=?",trxName)
						.setParameters(product.get_ID(),0,false)
						.setOrderBy(MWM_EmptyStorageLine.COLUMNNAME_DateStart+" DESC")
						.list();
		}else{
			elines = new Query(Env.getCtx(),MWM_EmptyStorageLine.Table_Name,MWM_EmptyStorageLine.COLUMNNAME_M_Product_ID+"=? AND "+MWM_EmptyStorageLine.COLUMNNAME_QtyMovement+">? AND ISSOTRX=?",trxName)
					.setParameters(product.get_ID(),0,false)
					.setOrderBy(product.getGuaranteeDays()>0?MWM_EmptyStorageLine.COLUMNNAME_DateStart:MWM_EmptyStorageLine.COLUMNNAME_DateStart+" DESC")
					.list();
		}
	
		if (elines==null){
			log.severe("Product has no Storage available to pick: "+product.getName());
			return false;
		}
			
		BigDecimal binPickQty = line.getQtyOrdered();//.multiply(new BigDecimal(product.getUnitsPerPack()));
		for (MWM_EmptyStorageLine eline:elines){
			if (eline.getWM_InOutLine().getM_InOutLine_ID()<1 && line.isReceived())
				throw new AdempiereException("This Product Has No Shipment/Receipt record. Complete its WM Inout first before picking - "+product.getName()+" -> "+eline.getWM_InOutLine());
			
			//cannot take Blocked
			MWM_EmptyStorage storage = new Query(Env.getCtx(),MWM_EmptyStorage.Table_Name,MWM_EmptyStorage.COLUMNNAME_WM_EmptyStorage_ID+"=?",trxName)
					.setParameters(eline.getWM_EmptyStorage_ID())
					.first();
			if (storage.isBlocked())
				continue;
			
			//take those that are Complete DocStatus (Putaway) or no HandlingUnit
			MWM_HandlingUnit hu = new Query(Env.getCtx(),MWM_HandlingUnit.Table_Name,MWM_HandlingUnit.COLUMNNAME_DocStatus+"=? AND "+MWM_HandlingUnit.COLUMNNAME_WM_HandlingUnit_ID+"=?",trxName)
					.setParameters(MWM_HandlingUnit.DOCSTATUS_Completed,eline.getWM_HandlingUnit_ID())
					.first();
			if (hu==null && line.isReceived())
				continue; //next EmptyLine until not InProgress
			
			//TODO Eline.QtyMovement is not pure (need to include sub ESLine Qty)
			//Locator EmptyLine Quantity has more than what you picking
			if (eline.getQtyMovement().compareTo(binPickQty)>=0){ 
				binPickQty = startPickingProcess(binPickQty,inout,line, eline);
				pickings++;
				return true;
				
			//Locator EmptyLine Quantity has less than what you picking	
			}else if(!IsSameLine) { //if not SameLine 
				binPickQty = binPickQty.subtract(startPickingProcess(eline.getQtyMovement(),inout,line, eline));
				pickings++;
			}  
		}
		return false;
	}

	private BigDecimal startPickingProcess(BigDecimal picked, MWM_InOut inout, MWM_DeliveryScheduleLine line,MWM_EmptyStorageLine eline) {
		
		MWM_EmptyStorage empty = (MWM_EmptyStorage) eline.getWM_EmptyStorage();
		
		//Locator EmptyLine Quantity has more than what you picking
		if (eline.getQtyMovement().compareTo(picked)>0){
			//if got handling unit, then assign the minor picked to new handling unit. Otherwise reject this reset
			if (WM_HandlingUnit_ID>0){
				MWM_InOutLine inoutline = util.newInOutLine(inout,line,picked); 
				setLocator(inoutline, eline.getWM_EmptyStorage().getM_Locator_ID());				
//red1				MWM_EmptyStorageLine newline = util.newEmptyStorageLine(line, picked, empty, inoutline);
//				if (isReceived) {
					//util.releaseHandlingUnit(eline);
					inoutline = util.assignHandlingUnit(IsSameDistribution,inoutline, null, picked);
//				}//
//red1				util.createESLinePicking(eline, newline);
				
		/*		if (isReceived) {//As in WM_DeliveryScheduleLineDocEvent line 68
					//deduct from eline and close off linked picking line
					BigDecimal pickFactor = picked.divide(new BigDecimal(eline.getM_Product().getUnitsPerPack()),2,RoundingMode.HALF_EVEN);			
					BigDecimal vacancy = empty.getAvailableCapacity().add(pickFactor);
					vacancy = vacancy.subtract(util.getFutureStorage(empty, line));
					empty.setAvailableCapacity(vacancy);
//red1					util.pickedEmptyStorageLine(inoutline,newline);
				}*/
				picked = Env.ZERO;//picking finished
			}else { 
				return picked;//go next EmptyLine until ExactFit
			}
			
		//Locator EmptyLine Quantity has exactly same size what you picking	
		} else {
			MWM_InOutLine inoutline = util.newInOutLine(inout,line,picked); 
			setLocator(inoutline, eline.getWM_EmptyStorage().getM_Locator_ID());
	/*		if (isReceived){
				eline.setDateEnd(inoutline.getUpdated());
				eline.setQtyMovement(Env.ZERO);
				if (WM_HandlingUnit_ID>0){
					util.releaseHandlingUnit(eline);
					util.setHandlingUnit(WM_HandlingUnit_ID); 
					inoutline = util.assignHandlingUnit(IsSameDistribution,inoutline, eline, picked);
				}
			} else
				eline.setDateEnd(line.getWM_DeliverySchedule().getDatePromised());
			
			eline.saveEx(trxName);*/
		}
		//if (isReceived)	
		//	util.calculatePercentageVacant(line.isReceived(),empty);//empty.saved() there  
		return picked;
	}

	private boolean orderLineWarehousePick(MWM_InOut inout, MWM_DeliveryScheduleLine line) {
		MWM_WarehousePick wp = new Query(Env.getCtx(),MWM_WarehousePick.Table_Name,MWM_WarehousePick.COLUMNNAME_C_OrderLine_ID+"=?",trxName)
				.setParameters(line.getC_OrderLine_ID())
				.first();
		if (wp!=null){
			MWM_EmptyStorageLine sel = new Query(Env.getCtx(), MWM_EmptyStorageLine.Table_Name,MWM_EmptyStorageLine.COLUMNNAME_WM_EmptyStorageLine_ID+"=? AND "
					+MWM_EmptyStorageLine.COLUMNNAME_M_Product_ID+"=?", trxName)
					.setParameters(wp.getWM_EmptyStorageLine_ID(),wp.getM_Product_ID())
					.first();
			if (sel==null){
				log.severe("WarehousePick by Sales OrderLine is lost!:"+wp.toString());
			}else {
				BigDecimal picked = sel.getQtyMovement();
				if (picked.compareTo(wp.getQtyOrdered())==0) { 
					picked = startPickingProcess(picked,inout, line, sel);
				}
				if (picked.compareTo(Env.ZERO)==0){
					wp.setDescription(wp.getDescription()+" SUCCESS DURING PICKING!"); 
					wp.saveEx(trxName);
					return true;
				}else {
					log.severe("Cannot pickingEmptyStorage - "+sel.toString());
					return false;
			}}}
		return false;
	}

}
