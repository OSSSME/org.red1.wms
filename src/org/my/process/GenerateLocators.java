/*** Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.* If you shall try to cheat and find a loophole in this license, then KARMA will exact your share,* and your worldly gain shall come to naught and those who share shall gain eventually above you.* In compliance with previous GPLv2.0 works of Jorg Janke, Low Heng Sin, Carlos Ruiz and contributors.* This Module Creator is an idea put together and coded by Redhuan D. Oon (red1@red1.org)*/package org.my.process;
import java.util.List;import org.adempiere.exceptions.AdempiereException;import org.compiere.model.MLocator;import org.compiere.model.MStorageOnHand;import org.compiere.model.MWarehouse;import org.compiere.model.Query;import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;import org.compiere.util.Env;

	public class GenerateLocators extends SvrProcess {
	private int M_Warehouse_ID = 0;
	private String X = "";
	private String Y = "";
	private String Z = "";			protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
			for (ProcessInfoParameter p:para) {
				String name = p.getParameterName();
				if (p.getParameter() == null)					;
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
		}
	}	
	private StringBuilder LocatorValueName = new StringBuilder();	protected String doIt() {		int cnt = 0;		if (X==null || Y==null || Z==null || (X+Y+Z).isEmpty()){			throw new AdempiereException("X/Y/Z must be set to maximum dimensions");		} 		if (M_Warehouse_ID<1){			List<MWarehouse> whses = new Query(Env.getCtx(),MWarehouse.Table_Name,"",get_TrxName())					.setClient_ID().setOnlyActiveRecords(true).list();			for (MWarehouse whse:whses){				cnt = createLocators(cnt, whse);			}		}else{			MWarehouse wh = new Query(Env.getCtx(),MWarehouse.Table_Name,MWarehouse.COLUMNNAME_M_Warehouse_ID+"=?",get_TrxName())					.setParameters(M_Warehouse_ID)					.setClient_ID()					.first();			cnt = createLocators(cnt,wh);		}
		return "Locators Created "+cnt+", Last Locator Value: "+LocatorValueName.toString();
	}	private int createLocators(int cnt, MWarehouse whse) { 		//delete old locators first?		List<MLocator> oldlocs = new Query(Env.getCtx(),MLocator.Table_Name,MLocator.COLUMNNAME_M_Warehouse_ID+"=? AND "+MLocator.COLUMNNAME_M_Locator_ID+">999999",get_TrxName())				.setParameters(whse.get_ID())				.setClient_ID()				.list();		for (MLocator loc:oldlocs){			MStorageOnHand soh = new Query(Env.getCtx(),MStorageOnHand.Table_Name,MStorageOnHand.COLUMNNAME_M_Locator_ID+"=?",get_TrxName())					.setParameters(loc.get_ID())					.first();			if (soh==null)				loc.delete(false);		}		MLocator locator = null;		for (int i=1;i<new Integer(X).intValue()+1;i++){			for (int j=1;j<new Integer(Y).intValue()+1;j++){				for (int k=1;k<new Integer(Z).intValue()+1;k++){					LocatorValueName = new StringBuilder(whse.getValue()+"-"+i+"-"+j+"-"+k);					locator = new MLocator(whse,LocatorValueName.toString());					locator.setXYZ(new Integer(i).toString(), new Integer(j).toString(), new Integer(k).toString());					locator.saveEx(get_TrxName());					cnt++;				}			}		}		if (locator!=null){			whse.setM_ReserveLocator_ID(locator.get_ID());			whse.saveEx(get_TrxName());		}		return cnt;	}
}
