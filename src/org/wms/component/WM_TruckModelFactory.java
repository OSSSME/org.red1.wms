/**
import java.sql.ResultSet;
import org.adempiere.base.IModelFactory;
import org.compiere.model.PO;
import org.compiere.util.Env;

public class WM_TruckModelFactory implements IModelFactory {
	@Override 	public Class<?> getClass(String tableName) {
		 if (tableName.equals(MWM_Truck.Table_Name)){
			 return MWM_Truck.class;
		 }
  		return null;
	}
	@Override	public PO getPO(String tableName, int Record_ID, String trxName) {
		 if (tableName.equals(MWM_Truck.Table_Name)) {
		     return new MWM_Truck(Env.getCtx(), Record_ID, trxName);
		 }
  		return null;
	}
	@Override	public PO getPO(String tableName, ResultSet rs, String trxName) {
		 if (tableName.equals(MWM_Truck.Table_Name)) {
		     return new MWM_Truck(Env.getCtx(), rs, trxName);
		   }
 		 return null;
	}
}