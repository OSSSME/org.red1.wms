/*** Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.* If you shall try to cheat and find a loophole in this license, then KARMA will exact your share,* and your worldly gain shall come to naught and those who share shall gain eventually above you.* In compliance with previous GPLv2.0 works of Jorg Janke, Low Heng Sin, Carlos Ruiz and contributors.* This Module Creator is an idea put together and coded by Redhuan D. Oon (red1@red1.org)*/package org.my.process;
import org.compiere.process.ProcessInfoParameter;
import java.util.List;
import org.compiere.model.Query;
import org.compiere.util.Env;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import org.compiere.util.DB;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MSequence;
import org.my.model.MWM_InOutLine;
import org.compiere.process.SvrProcess;

	public class PutawayHandlingUnit extends SvrProcess {
	private int WM_HandlingUnit_ID = 0;
	private boolean IsSameDistribution = false;
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
			for (ProcessInfoParameter p:para) {
				String name = p.getParameterName();
				if (p.getParameter() == null)					;
				else if(name.equals("WM_HandlingUnit_ID")){
					WM_HandlingUnit_ID = p.getParameterAsInt();
			}
				else if(name.equals("IsSameDistribution")){
					IsSameDistribution = "Y".equals(p.getParameter());
			}
		}
	}
	protected String doIt() {
		String whereClause = "EXISTS (SELECT T_Selection_ID FROM T_Selection WHERE T_Selection.AD_PInstance_ID=? AND T_Selection.T_Selection_ID=WM_InOutLine.WM_InOutLine_ID)";

		List<MWM_InOutLine> lines = new Query(Env.getCtx(),MWM_InOutLine.Table_Name,whereClause,get_TrxName())
		.setParameters(getAD_PInstance_ID()).list();

		for (MWM_InOutLine line:lines){
			int a = line.get_ID();

			log.info("Selected line ID = "+a);

	}

	return "RESULT: "+lines.toString();

	}
}
