/*** Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.* If you shall try to cheat and find a loophole in this license, then KARMA will exact your share,* and your worldly gain shall come to naught and those who share shall gain eventually above you.* In compliance with previous GPLv2.0 works of Jorg Janke, Low Heng Sin, Carlos Ruiz and contributors.* This Module Creator is an idea put together and coded by Redhuan D. Oon (red1@red1.org)*/package org.my.process;
import org.compiere.process.ProcessInfoParameter;
import java.util.List;
import org.compiere.model.Query;
import org.compiere.util.Env;
import java.sql.SQLException;import java.math.BigDecimal;
import java.sql.PreparedStatement;
import org.compiere.util.DB;
import org.adempiere.exceptions.AdempiereException;import org.compiere.model.MProduct;
import org.compiere.model.MSequence;import org.my.model.MWM_EmptyStorage;import org.my.model.MWM_EmptyStorageLine;
import org.my.model.MWM_InOutLine;import org.my.model.MWM_PreferredProduct;import org.my.model.MWM_ProductType;import org.my.model.MWM_StorageType;
import org.compiere.process.SvrProcess;

	public class PutawayLocator extends SvrProcess {
	private int WM_Gate_ID = 0;
	private int M_Warehouse_ID = 0;
	private String X = "";
	private String Y = "";
	private String Z = "";
	private boolean ExactMatch = false;
	private int WM_RouteLocation_ID = 0;
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
			for (ProcessInfoParameter p:para) {
				String name = p.getParameterName();
				if (p.getParameter() == null)					;
				else if(name.equals("WM_Gate_ID")){
					WM_Gate_ID = p.getParameterAsInt();
			}
				else if(name.equals("M_Warehouse_ID")){
					M_Warehouse_ID = p.getParameterAsInt();
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
				else if(name.equals("ExactMatch")){
					ExactMatch = "Y".equals(p.getParameter());
			}
				else if(name.equals("WM_RouteLocation_ID")){
					WM_RouteLocation_ID = p.getParameterAsInt();
			}
		}
	}
	protected String doIt() {		if (X==null || Y==null || Z==null || (X+Y+Z).isEmpty()){			;		}else {			if (M_Warehouse_ID < 1)				throw  new AdempiereException("Select Warehouse if X,Y,Z has values"); 		}
		String whereClause = "EXISTS (SELECT T_Selection_ID FROM T_Selection WHERE T_Selection.AD_PInstance_ID=? AND T_Selection.T_Selection_ID=WM_InOutLine.WM_InOutLine_ID)";

		List<MWM_InOutLine> lines = new Query(Env.getCtx(),MWM_InOutLine.Table_Name,whereClause,get_TrxName())
		.setParameters(getAD_PInstance_ID()).list();

		for (MWM_InOutLine line:lines){
			int a = line.get_ID();

			log.info("Selected line ID = "+a);
			//get Product from InOut Bound line			MProduct product = (MProduct) line.getM_Product();						//check if defined in PreferredProduct...			List<MWM_PreferredProduct> preferreds = new Query(Env.getCtx(),MWM_PreferredProduct.Table_Name,MWM_PreferredProduct.COLUMNNAME_M_Product_ID+"=?" ,get_TrxName())					.setParameters(product.get_ID())					.setOrderBy(MWM_PreferredProduct.COLUMNNAME_M_Locator_ID)					.list();			if (preferreds!=null){				for (MWM_PreferredProduct preferred:preferreds){					if (preferred.getM_Locator().getM_Warehouse_ID()!=M_Warehouse_ID)						continue;					if (preferred.getM_Locator().getX().compareTo(X)>0 || preferred.getM_Locator().getY().compareTo(Y)>0 ||preferred.getM_Locator().getZ().compareTo(Z)>0)						continue;										//get first EmptyStorage					int locator_id = preferred.getM_Locator_ID();					int putaway = getSuitableEmptyStorage(line,locator_id,product);						if (putaway>0){						setPutawayLocator(line,putaway);						break;					}										}			} 						//get ProductType = StorageType			MWM_ProductType prodtype = new Query(Env.getCtx(),MWM_ProductType.Table_Name,MWM_ProductType.COLUMNNAME_M_Product_ID+"=?",get_TrxName())					.setParameters(product.get_ID())					.first();			 				String prodtypestring = prodtype.getTypeString();				if (prodtypestring==null || prodtypestring.isEmpty())					throw new AdempiereException("RUN Set Type String for faster processing");								List<MWM_StorageType> stortypes= new Query(Env.getCtx(),MWM_StorageType.Table_Name,MWM_StorageType.COLUMNNAME_TypeString+"=? AND "+(M_Warehouse_ID>0?MWM_StorageType.COLUMNNAME_M_Warehouse_ID+"=?":""),get_TrxName())						.setParameters(prodtypestring,M_Warehouse_ID>0?M_Warehouse_ID:"")						.setOrderBy("Created")						.list();								for (MWM_StorageType stortype:stortypes){					if (stortype!=null){						int located = getSuitableEmptyStorage(line, stortype.getM_Locator_ID(), product);						if (located>0){							setPutawayLocator(line,located);							break;					}				}				} 	 			//get non reserved empty storage			List<MWM_EmptyStorage> empties = new Query(Env.getCtx(),MWM_EmptyStorage.Table_Name,MWM_EmptyStorage.COLUMNNAME_IsFull+"=?",get_TrxName())				.setParameters('N')				.list();							if (empties==null)				throw new AdempiereException("NO MORE EMPTY STORAGE");						for (MWM_EmptyStorage empty:empties){				if (empty.getM_Locator().getX().compareTo(X)>0 || empty.getM_Locator().getY().compareTo(Y)>0  || empty.getM_Locator().getZ().compareTo(Z)>0 )						continue;				int located = getSuitableEmptyStorage(line, empty.getM_Locator_ID(), product);				if (located>0){					setPutawayLocator(line,located);					break;				}			}
	}

	return "RESULT: "+lines.toString();

	}	private void setPutawayLocator(MWM_InOutLine line, int putaway) { 		line.setM_Locator_ID(putaway);		line.saveEx(get_TrxName());	}	private int getSuitableEmptyStorage(MWM_InOutLine line,int locator_id, MProduct product){		MWM_EmptyStorage empty = new Query(Env.getCtx(),MWM_EmptyStorage.Table_Name,MWM_EmptyStorage.COLUMNNAME_IsFull+"=? AND "				+MWM_EmptyStorage.COLUMNNAME_M_Locator_ID+"=?",get_TrxName())				.setParameters('N',locator_id).first();		if (empty==null)			return 0;		//found an empty. Now measure dimensions to fit Quant, or next  		BigDecimal vacantcapacity = empty.getVacantCapacity(); 		if (vacantcapacity==null) 			throw new AdempiereException("Bin Vacant Capacity not set"); 		vacantcapacity = vacantcapacity.subtract(new BigDecimal(line.getQtyPicked())); 		if (vacantcapacity.compareTo(Env.ZERO)>0) 			addEmptyStorageLine(line, empty); 		else return 0;		 		empty.setVacantCapacity(vacantcapacity); 		 		if (vacantcapacity.compareTo(Env.ZERO)==0){ 			//EmptyStorage is full 			empty.setIsFull(true); 			empty.saveEx(get_TrxName()); 		} 				return locator_id;				}		private void addEmptyStorageLine(MWM_InOutLine line, MWM_EmptyStorage empty) {		MWM_EmptyStorageLine newemptyline = new MWM_EmptyStorageLine(Env.getCtx(),0,get_TrxName());		newemptyline.setWM_EmptyStorage_ID(empty.get_ID());		newemptyline.setQtyMovement(line.getQtyPicked());		newemptyline.setWM_InOutLine_ID(line.get_ID());		newemptyline.setM_Product_ID(line.getM_Product_ID());		newemptyline.saveEx(get_TrxName());	}
}
