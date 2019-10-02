/**

import org.compiere.model.Query;
import org.compiere.util.Env;
import java.sql.PreparedStatement;
import org.compiere.util.DB;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MSequence;
import org.wms.model.MWM_DeliveryScheduleLine;
import org.compiere.process.SvrProcess;

	public class CheckAvailability extends SvrProcess {
	Timestamp earliest = new Timestamp (System.currentTimeMillis()); 
	int M_Warehouse_ID = 0; 
		ProcessInfoParameter[] para = getParameter();
			for (ProcessInfoParameter p:para) {
				String name = p.getParameterName();
				if (p.getParameter() == null)
				else if(name.equals("IsActive")){
					IsActive = "Y".equals(p.getParameter());
			}
		}
	}

		String whereClause = "EXISTS (SELECT T_Selection_ID FROM T_Selection WHERE T_Selection.AD_PInstance_ID=? AND T_Selection.T_Selection_ID=WM_DeliveryScheduleLine.WM_DeliveryScheduleLine_ID)";
		.setParameters(getAD_PInstance_ID()).list();
		if (lines.isEmpty())
		for (MWM_DeliveryScheduleLine line:lines){
			if (isAvailable(line) && IsActive)
	}

	return "All selected are available "+(IsActive?" FIFO Set "+earliest:"");

	}
}