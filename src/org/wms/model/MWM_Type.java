/**

import java.sql.ResultSet;
import java.util.Properties;

public class MWM_Type extends X_WM_Type{

	private static final long serialVersionUID = -1L;

	public MWM_Type(Properties ctx, int id, String trxName) {
		super(ctx,id,trxName);
		}

	public MWM_Type(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
}