/**

import java.sql.ResultSet;
import java.util.Properties;

public class MWM_EmptyStorageLine extends X_WM_EmptyStorageLine{

	private static final long serialVersionUID = -1L;

	public MWM_EmptyStorageLine(Properties ctx, int id, String trxName) {
		super(ctx,id,trxName);
		}

	public MWM_EmptyStorageLine(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
}