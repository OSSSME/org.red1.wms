/*** Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.* If you shall try to cheat and find a loophole in this license, then KARMA will exact your share,* and your worldly gain shall come to naught and those who share shall gain eventually above you.* In compliance with previous GPLv2.0 works of Jorg Janke, Low Heng Sin, Carlos Ruiz and contributors.* This Module Creator is an idea put together and coded by Redhuan D. Oon (red1@red1.org)*/package org.my.process;
 import java.util.List;import org.adempiere.exceptions.AdempiereException;import org.compiere.model.MAttributeSetInstance;import org.compiere.model.MProduct;import org.compiere.model.Query;import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;import org.compiere.util.Env;import org.my.model.MWM_DeliverySchedule;import org.my.model.MWM_DeliveryScheduleLine;import org.my.model.MWM_InOut;import org.my.model.MWM_InOutLine;import org.my.model.X_WM_DeliverySchedule; 

	public class GeneratePutawayList extends SvrProcess {
	private int WM_Gate_ID = 0;
	private int M_Warehouse_ID = 0;
	private String X = "";
	private String Y = "";
	private String Z = "";
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
		}
	}
	/**	 * GO thru Approved DeliverySchedules and copy to WM_InOut inbound for Putaway process.	 * Change DocStatus to Complete when successful.	 * Select by Gate/Warehouse location of goods or by Product or Vendor. or by manual selection in Info-Window	 * Each selection in own Putaway List (WM_InOut)	 */	protected String doIt() {		if (WM_Gate_ID<0 && M_Warehouse_ID<0)			throw new AdempiereException("Gate Or Warehouse Must Be Set");		String where = M_Warehouse_ID>0?MWM_DeliverySchedule.COLUMNNAME_M_Warehouse_ID+"=? AND ":MWM_DeliverySchedule.COLUMNNAME_WM_Gate_ID+"=? AND ";				List<MWM_DeliverySchedule> dev = new Query(Env.getCtx(),MWM_DeliverySchedule.Table_Name,where+MWM_DeliverySchedule.COLUMNNAME_DocStatus+"=?",get_TrxName())				.setParameters(M_Warehouse_ID>0?M_Warehouse_ID:WM_Gate_ID,X_WM_DeliverySchedule.DOCACTION_Approve)				.list();		if (dev==null)			return"";		//create WM_InOut header		MWM_InOut header = new MWM_InOut(Env.getCtx(),0,get_TrxName()); 		header.setWM_DeliverySchedule_ID(dev.get(0).getWM_DeliverySchedule_ID());		header.setWM_Gate_ID(dev.get(0).getWM_Gate_ID());		header.saveEx(get_TrxName());				for (MWM_DeliverySchedule d:dev){			List<MWM_DeliveryScheduleLine> schlines = new Query(Env.getCtx(),MWM_DeliveryScheduleLine.Table_Name,MWM_DeliveryScheduleLine.COLUMNNAME_WM_DeliverySchedule_ID+"=?",get_TrxName())					.setParameters(d.get_ID())					.list();			for (MWM_DeliveryScheduleLine sch:schlines){				MWM_InOutLine inline = new MWM_InOutLine(Env.getCtx(),0,get_TrxName());				inline.setWM_InOut_ID(header.get_ID());				inline.setM_Product_ID(sch.getM_Product_ID());				inline.setC_OrderLine_ID(sch.getC_OrderLine_ID());				inline.saveEx(get_TrxName());							}		}		
		return "DONE";
	}	private void createWarehouseASI(MProduct product){		MAttributeSetInstance asi = MAttributeSetInstance.create(Env.getCtx(), product, get_TrxName());	}
}
