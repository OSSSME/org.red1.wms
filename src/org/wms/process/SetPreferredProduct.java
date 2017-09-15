/**

import org.compiere.model.Query;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;

	public class SetPreferredProduct extends SvrProcess {



		ProcessInfoParameter[] para = getParameter();
			for (ProcessInfoParameter p:para) {
				String name = p.getParameterName();
				if (p.getParameter() == null)
				else if(name.equals("M_Product_ID")){
					M_Product_ID = p.getParameterAsInt();
			}
				else if(name.equals("DeleteOld")){
					DeleteOld = (Boolean)p.getParameterAsBoolean();
			}
		}
	}

		String whereClause = "EXISTS (SELECT T_Selection_ID FROM T_Selection WHERE T_Selection.AD_PInstance_ID=? AND T_Selection.T_Selection_ID=M_Locator.M_Locator_ID)";

		List<MLocator> lines = new Query(Env.getCtx(),MLocator.Table_Name,whereClause,get_TrxName())
		.setParameters(getAD_PInstance_ID()).list();

		for (MLocator line:lines){
		}

		return "Preferred Product Set - No. Of Locators: "+cnt;
	}
}