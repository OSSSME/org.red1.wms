/*** Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.* If you shall try to cheat and find a loophole in this license, then KARMA will exact your share,* and your worldly gain shall come to naught and those who share shall gain eventually above you.* In compliance with previous GPLv2.0 works of Jorg Janke, Low Heng Sin, Carlos Ruiz and contributors.* This Module Creator is an idea put together and coded by Redhuan D. Oon (red1@red1.org)*/package org.my.process;
import org.compiere.process.ProcessInfoParameter;
import java.util.List;
import org.compiere.model.Query;
import org.compiere.util.Env;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import org.compiere.util.DB;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MSequence;import org.my.model.MWM_EmptyStorageLine;import org.my.model.MWM_HandlingUnit;import org.my.model.MWM_HandlingUnitHistory;
import org.my.model.MWM_InOutLine;
import org.compiere.process.SvrProcess;

	public class AssignHandlingUnit extends SvrProcess {
	private int WM_HandlingUnit_ID = 0;
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
			for (ProcessInfoParameter p:para) {
				String name = p.getParameterName();
				if (p.getParameter() == null)					;
				else if(name.equals("WM_HandlingUnit_ID")){
					WM_HandlingUnit_ID = p.getParameterAsInt();
			}
		}
	}
	protected String doIt() {
		String whereClause = "EXISTS (SELECT T_Selection_ID FROM T_Selection WHERE T_Selection.AD_PInstance_ID=? AND T_Selection.T_Selection_ID=WM_InOutLine.WM_InOutLine_ID)";

		List<MWM_InOutLine> lines = new Query(Env.getCtx(),MWM_InOutLine.Table_Name,whereClause,get_TrxName())
		.setParameters(getAD_PInstance_ID()).list();
		int old = 0;		int units = 0;		
		for (MWM_InOutLine line:lines){			//if previous handling unit, then release/replace it fully 			if (line.getWM_HandlingUnit_ID()>0){				MWM_HandlingUnit oldhu = new Query(Env.getCtx(),MWM_HandlingUnit.Table_Name,MWM_HandlingUnit.COLUMNNAME_WM_HandlingUnit_ID+"=?",get_TrxName())						.setParameters(line.getWM_HandlingUnit_ID())						.first();				oldhu.setQtyMovement(Env.ZERO);				oldhu.setDocStatus(MWM_HandlingUnit.DOCSTATUS_Drafted);				oldhu.saveEx(get_TrxName());				old++;				//history				MWM_HandlingUnitHistory oldhuh = new Query(Env.getCtx(),MWM_HandlingUnitHistory.Table_Name,MWM_HandlingUnitHistory.COLUMNNAME_WM_HandlingUnit_ID+"=?",get_TrxName())						.setParameters(oldhu.getWM_HandlingUnit_ID())						.first();				if (oldhuh!=null){					oldhuh.setDateEnd(oldhu.getUpdated());					oldhuh.saveEx(get_TrxName());				} else					log.severe("NO HandlingUnit History for: "+oldhu.getName());			}			//Replacing with user selected HU			MWM_HandlingUnit hu =  null;
			hu = new Query(Env.getCtx(),MWM_HandlingUnit.Table_Name,MWM_HandlingUnit.COLUMNNAME_WM_HandlingUnit_ID+">=? AND ("					+MWM_HandlingUnit.COLUMNNAME_QtyMovement+"=? OR QtyMovement IS NULL)",get_TrxName())					.setParameters(WM_HandlingUnit_ID,Env.ZERO)					.setOrderBy(MWM_HandlingUnit.COLUMNNAME_WM_HandlingUnit_ID)					.first();			if (hu==null)				return "NO MORE HandlingUnits. Limited to remaining HandlingUnits. OLD UNITS RELEASED: "+old+" NEW UNITS ASSIGNED: "+units;			hu.setQtyMovement(line.getQtyPicked());			hu.setDocStatus(line.getWM_InOut().isSOTrx()?MWM_HandlingUnit.DOCSTATUS_InProgress:MWM_HandlingUnit.DOCSTATUS_Completed);			hu.setM_Product_ID(line.getM_Product_ID());			hu.setM_Locator_ID(line.getM_Locator_ID());			hu.setC_UOM_ID(line.getC_UOM_ID());			hu.saveEx(get_TrxName());				WM_HandlingUnit_ID=hu.get_ID();			units++;			//HandlingUnit History			MWM_HandlingUnitHistory huh = new MWM_HandlingUnitHistory(Env.getCtx(),0,get_TrxName());			huh.setWM_HandlingUnit_ID(hu.get_ID());			huh.setWM_InOutLine_ID(line.get_ID());			huh.setDateStart(hu.getCreated());			huh.setC_Order_ID(line.getWM_DeliveryScheduleLine().getWM_DeliverySchedule().getC_Order_ID());			huh.setM_Product_ID(line.getM_Product_ID());			huh.setC_UOM_ID(line.getC_UOM_ID());			huh.saveEx(get_TrxName());						//Linking with WM_InOutLine and EmptyStorageLine			line.setWM_HandlingUnit_ID(WM_HandlingUnit_ID);			line.saveEx(get_TrxName());						MWM_EmptyStorageLine eline = new Query(Env.getCtx(),MWM_EmptyStorageLine.Table_Name,MWM_EmptyStorageLine.COLUMNNAME_WM_InOutLine_ID+"=?",get_TrxName())					.setParameters(line.get_ID())					.first();			if (eline==null)				log.severe("InOutLine has no EmptyStorageLine: "+line.toString());			else {				eline.setWM_HandlingUnit_ID(WM_HandlingUnit_ID);				eline.saveEx(get_TrxName());			}
		}

	return "OLD UNITS RELEASED: "+old+" NEW UNITS ASSIGNED: "+units;

	}
}
