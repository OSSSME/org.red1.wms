/**

import java.sql.ResultSet;
import java.util.Properties;

public class MWM_RouteLocation extends X_WM_RouteLocation{

	private static final long serialVersionUID = -1L;

	public MWM_RouteLocation(Properties ctx, int id, String trxName) {
		super(ctx,id,trxName);
		}

	public MWM_RouteLocation(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
}