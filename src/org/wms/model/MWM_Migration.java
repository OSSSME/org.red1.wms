/**

import java.sql.ResultSet;
import java.util.Properties;

public class MWM_Migration extends X_WM_Migration{

	private static final long serialVersionUID = -1L;

	public MWM_Migration(Properties ctx, int id, String trxName) {
		super(ctx,id,trxName);
		}

	public MWM_Migration(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
}