/**

import java.sql.ResultSet;
import java.util.Properties; 

public class MWM_WarehousePick extends X_WM_WarehousePick{

	private static final long serialVersionUID = -1L;

	public MWM_WarehousePick(Properties ctx, int id, String trxName) {
		super(ctx,id,trxName);
		}

	public MWM_WarehousePick(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
}