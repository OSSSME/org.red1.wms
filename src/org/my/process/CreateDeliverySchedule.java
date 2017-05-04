/*** Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.* If you shall try to cheat and find a loophole in this license, then KARMA will exact your share,* and your worldly gain shall come to naught and those who share shall gain eventually above you.* In compliance with previous GPLv2.0 works of Jorg Janke, Low Heng Sin, Carlos Ruiz and contributors.* This Module Creator is an idea put together and coded by Redhuan D. Oon (red1@red1.org)*/package org.my.process;
import org.compiere.process.ProcessInfoParameter;
import java.util.List;
import org.compiere.model.Query;
import org.compiere.util.Env;import org.my.model.MWM_DeliverySchedule;import org.my.model.MWM_DeliveryScheduleLine;import java.sql.SQLException;import java.sql.Timestamp;
import java.sql.PreparedStatement;
import org.compiere.util.DB;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MSequence;
import org.compiere.model.MOrderLine;
import org.compiere.process.SvrProcess;

	public class CreateDeliverySchedule extends SvrProcess {
	private int WM_Gate_ID = 0;
	private Timestamp DatePromised = null;
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
			for (ProcessInfoParameter p:para) {
				String name = p.getParameterName();
				if (p.getParameter() == null)					;
				else if(name.equals("WM_Gate_ID")){
					WM_Gate_ID = p.getParameterAsInt();
			}
				else if(name.equals("DatePromised")){
					DatePromised = p.getParameterAsTimestamp();
			}
		}
	}
	protected String doIt() {		if (WM_Gate_ID<1 || DatePromised==null)			throw new AdempiereException("Set Gate and Date Promised");
		String whereClause = "EXISTS (SELECT T_Selection_ID FROM T_Selection WHERE T_Selection.AD_PInstance_ID=? AND T_Selection.T_Selection_ID=C_OrderLine.C_OrderLine_ID)";

		List<MOrderLine> lines = new Query(Env.getCtx(),MOrderLine.Table_Name,whereClause,get_TrxName())
		.setParameters(getAD_PInstance_ID()).list();
				MWM_DeliverySchedule schedule = new Query(Env.getCtx(),MWM_DeliverySchedule.Table_Name,MWM_DeliverySchedule.COLUMNNAME_DatePromised+"=? AND "		+MWM_DeliverySchedule.COLUMNNAME_WM_Gate_ID+"=? AND "+MWM_DeliverySchedule.COLUMNNAME_C_Order_ID+"=?",get_TrxName())				.setParameters(DatePromised,WM_Gate_ID,lines.get(0).getC_Order_ID())				.first();		if (schedule!=null)			throw new AdempiereException("Already done same DateTime and Gate.");				schedule = new MWM_DeliverySchedule(Env.getCtx(), 0, get_TrxName());		schedule.setWM_Gate_ID(WM_Gate_ID);		schedule.setDatePromised(DatePromised);		schedule.setDateDelivered(DatePromised);		//schedule.setC_Order_ID(lines.get(0).getC_Order_ID());//TODO break at each different OrderID		schedule.setC_BPartner_ID(lines.get(0).getC_Order().getC_BPartner_ID());		schedule.setName(DatePromised.toString()+":"+schedule.getWM_Gate().getName());		schedule.saveEx(get_TrxName());		
		for (MOrderLine line:lines){
			int a = line.get_ID();

			log.info("Selected line ID = "+a);
						MWM_DeliveryScheduleLine dline = new MWM_DeliveryScheduleLine(Env.getCtx(), 0, get_TrxName());			dline.setWM_DeliverySchedule_ID(schedule.get_ID());			dline.setC_OrderLine_ID(line.getC_OrderLine_ID());			dline.setM_Product_ID(line.getM_Product_ID());			dline.setM_AttributeSetInstance_ID(line.getM_AttributeSetInstance_ID());			dline.setC_UOM_ID(line.getC_UOM_ID());			dline.setQtyOrdered(line.getQtyOrdered());			dline.saveEx(get_TrxName());			
		}

	return "RESULT: "+schedule.toString();

	}
}
