/**

import org.compiere.model.Query;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;
	/**
	public class StockMovement extends SvrProcess {

		ProcessInfoParameter[] para = getParameter();
			for (ProcessInfoParameter p:para) {
				String name = p.getParameterName();
				if (p.getParameter() == null)
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
		}
	}
	MWM_HandlingUnit hu = null;
		selection = selectionSQLwhere();
		for (MWM_EmptyStorageLine line:selection){
		}
		return "Lines done: "+done;
	}
}