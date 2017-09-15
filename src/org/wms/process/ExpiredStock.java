/**


import org.compiere.model.Query;
import org.compiere.util.Env;
import java.sql.PreparedStatement;
import org.compiere.util.DB;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MSequence;
import org.compiere.process.SvrProcess;

	public class ExpiredStock extends SvrProcess {









		ProcessInfoParameter[] para = getParameter();
			for (ProcessInfoParameter p:para) {
				String name = p.getParameterName();
				if (p.getParameter() == null)
				else if(name.equals("WM_HandlingUnit_ID")){
					WM_HandlingUnit_ID = p.getParameterAsInt();
			}
				else if(name.equals("Percent")){
					Percent = p.getParameterAsInt();
			}
				else if(name.equals("QtyMovement")){
					QtyMovement = p.getParameterAsInt();
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
				else if(name.equals("M_Locator_ID")){
					M_Locator_ID = p.getParameterAsInt();
			}
		}
	}

		String whereClause = "EXISTS (SELECT T_Selection_ID FROM T_Selection WHERE T_Selection.AD_PInstance_ID=? AND T_Selection.T_Selection_ID=WM_EmptyStorageLine.WM_EmptyStorageLine_ID)";

		List<MWM_EmptyStorageLine> lines = new Query(Env.getCtx(),MWM_EmptyStorageLine.Table_Name,whereClause,get_TrxName())
		.setParameters(getAD_PInstance_ID()).list();

		for (MWM_EmptyStorageLine line:lines){

	}

	return "RESULT: "+lines.toString();

	}
}