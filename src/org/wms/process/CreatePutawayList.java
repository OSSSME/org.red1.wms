/**


import org.compiere.model.Query;
import org.compiere.util.Env;
import java.sql.PreparedStatement;
import org.compiere.util.DB;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MSequence;
import org.compiere.process.SvrProcess;

	public class CreatePutawayList extends SvrProcess {
	private boolean IsSameLine = true;
	Utils util = null;
		ProcessInfoParameter[] para = getParameter();
			for (ProcessInfoParameter p:para) {
				String name = p.getParameterName();
				if (p.getParameter() == null)
				else if(name.equals("WM_HandlingUnit_ID")){
					WM_HandlingUnit_ID = p.getParameterAsInt();
				}		
				else if(name.equals("IsSameDistribution")){
					IsSameDistribution = "Y".equals(p.getParameter());
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
	/**
		String whereClause = "EXISTS (SELECT T_Selection_ID FROM T_Selection WHERE T_Selection.AD_PInstance_ID=? AND T_Selection.T_Selection_ID=WM_DeliveryScheduleLine.WM_DeliveryScheduleLine_ID)";
		List<MWM_DeliveryScheduleLine> lines = null;
		if (external){
}