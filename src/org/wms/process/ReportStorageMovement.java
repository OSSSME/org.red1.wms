/**


import org.compiere.model.Query;
import org.compiere.util.Env;
import java.sql.PreparedStatement;
import org.compiere.util.DB;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MSequence;
import org.compiere.process.SvrProcess;

	public class ReportStorageMovement extends SvrProcess {


		ProcessInfoParameter[] para = getParameter();
			for (ProcessInfoParameter p:para) {
				String name = p.getParameterName();
				if (p.getParameter() == null)
				else if(name.equals("IsActive")){
					IsActive = "Y".equals(p.getParameter());
			}
		}
	}

		MSequence seq = MSequence.get(getCtx(), "ReportStorageMovement");
		if (seq == null)
			throw new AdempiereException("No sequence for ReportStorageMovement table");
 		if (IsActive) { 
			String delete = "DELETE FROM ReportStorageMovement";
 			DB.executeUpdate(delete, get_TrxName());
 		}

 		String whereClause = "EXISTS (SELECT T_Selection_ID FROM T_Selection WHERE T_Selection.AD_PInstance_ID=? AND T_Selection.T_Selection_ID=WM_EmptyStorageLine.WM_EmptyStorageLine_ID)";

		List<MWM_EmptyStorageLine> lines = new Query(Env.getCtx(),MWM_EmptyStorageLine.Table_Name,whereClause,get_TrxName())
		.setParameters(getAD_PInstance_ID()).list();

		for (MWM_EmptyStorageLine line:lines){
			int a = line.get_ID();

			log.info("Selected line ID = "+a);

		String insert="INSERT INTO ReportStorageMovement (Value,WM_HandlingUnit_ID,M_Product_ID,QtyMovement,C_UOM_ID,GuaranteeDays,WM_InOut_ID,DateStart,DateEnd,X,Y,Z,IsSOTrx,C_Order_ID,M_Warehouse_ID,IsActive,AD_Client_ID,AD_Org_ID,Created,CreatedBy,Updated,UpdatedBy, ReportStorageMovement_ID) SELECT m.Value,a.WM_HandlingUnit_ID,p.M_Product_ID,a.QtyMovement,c.C_UOM_ID,p.GuaranteeDays,o.WM_InOut_ID,a.DateStart,a.DateEnd,m.X,m.Y,m.Z,a.IsSOTrx,e.C_Order_ID,m.M_Warehouse_ID,a.IsActive,a.AD_Client_ID,a.AD_Org_ID,a.Created,a.CreatedBy,a.Updated,a.UpdatedBy, nextIDFunc(?, 'N') FROM WM_EmptyStorageLine a " 
+"INNER JOIN WM_EmptyStorage s ON (s.WM_EmptyStorage_ID=a.WM_EmptyStorage_ID)"
		+" INNER JOIN M_Locator m ON (m.M_Locator_ID=s.M_Locator_ID) "
		+" INNER JOIN M_Product p ON (p.M_Product_ID=a.M_Product_ID) "
		+" LEFT JOIN WM_InOutLine c ON (c.WM_InOutLine_ID=a.WM_InOutLine_ID) "
		+" LEFT JOIN WM_InOut o ON (o.WM_InOut_ID=c.WM_InOut_ID) "
		+" LEFT JOIN WM_DeliveryScheduleLine d ON (d.WM_InOutLine_ID=c.WM_InOutLine_ID) "
		+" LEFT JOIN C_OrderLine e ON (e.C_OrderLine_ID=d.C_OrderLine_ID)  "
		+"  WHERE WM_EmptyStorageLine_ID ="+line.get_ID();

		PreparedStatement pstmt = null;
		try
		{
			pstmt = DB.prepareStatement(insert, get_TrxName());
			pstmt.setInt(1, seq.getAD_Sequence_ID());
			pstmt.executeUpdate();
		}
		catch (SQLException e)		{
			throw new AdempiereException(e);
		}
		finally {
			DB.close(pstmt);
			pstmt = null;
		}
	}

	return "RESULT: "+lines.toString();

	}
}