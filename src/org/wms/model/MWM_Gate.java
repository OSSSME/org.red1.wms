/**

import java.sql.ResultSet;
import java.util.Properties;

public class MWM_Gate extends X_WM_Gate{

	private static final long serialVersionUID = -1L;

	public MWM_Gate(Properties ctx, int id, String trxName) {
		super(ctx,id,trxName);
		}

	public MWM_Gate(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
}